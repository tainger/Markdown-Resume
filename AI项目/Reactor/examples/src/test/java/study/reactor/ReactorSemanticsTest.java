package study.reactor;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;
import reactor.test.publisher.TestPublisher;

import static org.junit.jupiter.api.Assertions.*;

/** 无模型、数据库或真实 HTTP 依赖的 Reactor 语义实验。 */
class ReactorSemanticsTest {
    @BeforeAll
    static void boundVerificationTime() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(3));
    }

    @Test
    void coldSourceExecutesForEachSubscription() {
        AtomicInteger calls = new AtomicInteger();
        Mono<Integer> source = Mono.fromSupplier(calls::incrementAndGet);
        assertEquals(0, calls.get());
        StepVerifier.create(source).expectNext(1).verifyComplete();
        StepVerifier.create(source).expectNext(2).verifyComplete();
    }

    @Test
    void justCapturesAnAlreadyEvaluatedValue() {
        AtomicInteger calls = new AtomicInteger();
        Mono<Integer> source = Mono.just(calls.incrementAndGet());
        assertEquals(1, calls.get());
        StepVerifier.create(source).expectNext(1).verifyComplete();
        StepVerifier.create(source).expectNext(1).verifyComplete();
        assertEquals(1, calls.get());
    }

    @Test
    void mapAndFlatMapComposeDifferentReturnTypes() {
        StepVerifier.create(Mono.just("hello").map(String::length)
                        .flatMap(length -> Mono.just(length * 2)))
                .expectNext(10).verifyComplete();
    }

    @Test
    void emptyAndVoidNeedCompletionComposition() {
        AtomicBoolean callback = new AtomicBoolean();
        Mono<Void> done = Mono.empty();
        StepVerifier.create(done.flatMap(ignored -> {
            callback.set(true);
            return Mono.just("unreachable");
        })).verifyComplete();
        assertFalse(callback.get());
        StepVerifier.create(done.thenReturn("OK")).expectNext("OK").verifyComplete();
    }

    @Test
    void zipNeedsAValueFromEachSource() {
        StepVerifier.create(Mono.zip(Mono.<String>empty(), Mono.just("B")))
                .verifyComplete();
    }

    @Test
    void whenCoordinatesVoidCompletions() {
        AtomicInteger actions = new AtomicInteger();
        StepVerifier.create(Mono.when(Mono.fromRunnable(actions::incrementAndGet),
                        Mono.fromRunnable(actions::incrementAndGet)))
                .verifyComplete();
        assertEquals(2, actions.get());
    }

    private static Mono<Integer> delayed(int value) {
        return Mono.delay(Duration.ofSeconds(4 - value)).thenReturn(value);
    }

    @Test
    void flatMapCanEmitInCompletionOrder() {
        StepVerifier.withVirtualTime(() -> Flux.just(1, 2, 3)
                        .flatMap(ReactorSemanticsTest::delayed, 3))
                .expectSubscription().thenAwait(Duration.ofSeconds(3))
                .expectNext(3, 2, 1).verifyComplete();
    }

    @Test
    void concatMapSerializesInnerSubscriptions() {
        List<Integer> completed = new ArrayList<>();
        StepVerifier.withVirtualTime(() -> Flux.just(1, 2, 3)
                        .concatMap(i -> delayed(i).doOnNext(completed::add)))
                .expectSubscription().thenAwait(Duration.ofSeconds(6))
                .expectNext(1, 2, 3).verifyComplete();
        assertEquals(List.of(1, 2, 3), completed);
    }

    @Test
    void flatMapSequentialOrdersOutputButNotSideEffects() {
        List<Integer> completed = new ArrayList<>();
        StepVerifier.withVirtualTime(() -> Flux.just(1, 2, 3)
                        .flatMapSequential(i -> delayed(i).doOnNext(completed::add), 3))
                .expectSubscription().thenAwait(Duration.ofSeconds(3))
                .expectNext(1, 2, 3).verifyComplete();
        assertEquals(List.of(3, 2, 1), completed);
    }

    @Test
    void downstreamExplicitlyRequestsValues() {
        StepVerifier.create(Flux.range(1, 3), 0)
                .thenRequest(1).expectNext(1)
                .thenRequest(2).expectNext(2, 3).verifyComplete();
    }

    @Test
    void recoveryReplacesTheFailedSequence() {
        StepVerifier.create(Flux.just(1, 0, 2).map(i -> 10 / i).onErrorReturn(-1))
                .expectNext(10, -1).verifyComplete();
    }

    @Test
    void recoveryInsideFlatMapCanPreserveOtherItems() {
        StepVerifier.create(Flux.just(1, 0, 2).concatMap(i ->
                        Mono.fromCallable(() -> 10 / i).onErrorReturn(-1)))
                .expectNext(10, -1, 5).verifyComplete();
    }

    @Test
    void retryResubscribesTwiceAfterInitialAttempt() {
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> request = Mono.defer(() -> attempts.incrementAndGet() < 3
                ? Mono.error(new IOException("temporary")) : Mono.just("OK"));
        StepVerifier.create(request.retry(2)).expectNext("OK").verifyComplete();
        assertEquals(3, attempts.get());
    }

    @Test
    void timeoutCanBeVerifiedWithoutRealWaiting() {
        StepVerifier.withVirtualTime(() -> Mono.never().timeout(Duration.ofHours(1)))
                .expectSubscription().thenAwait(Duration.ofHours(1))
                .expectError(TimeoutException.class).verify();
    }

    @Test
    void finalMessageIsBuiltAfterChunksAndStateIsPerSubscription() {
        Mono<String> response = Mono.defer(() -> {
            StringBuilder chunks = new StringBuilder();
            return Flux.just("你", "好")
                    .doOnNext(chunks::append)
                    .then(Mono.fromSupplier(chunks::toString));
        });
        StepVerifier.create(response).expectNext("你好").verifyComplete();
        StepVerifier.create(response).expectNext("你好").verifyComplete();
    }

    @Test
    void contextIsReadUpstreamAfterThreadSwitch() {
        Mono<String> identity = Mono.just("start")
                .publishOn(Schedulers.parallel())
                .flatMap(ignored -> Mono.deferContextual(ctx ->
                        Mono.just(ctx.<String>get("uid"))))
                .contextWrite(ctx -> ctx.put("uid", "u-42"));
        StepVerifier.create(identity).expectNext("u-42").verifyComplete();
    }

    @Test
    void innerContextWriteOverridesOuterForTheSource() {
        Mono<String> value = Mono.deferContextual(ctx -> Mono.just(ctx.<String>get("k")))
                .contextWrite(ctx -> ctx.put("k", "inner"))
                .contextWrite(ctx -> ctx.put("k", "outer"));
        StepVerifier.create(value).expectNext("inner").verifyComplete();
    }

    @Test
    void cancelPropagatesToTheReactiveSource() {
        AtomicBoolean cancelled = new AtomicBoolean();
        StepVerifier.create(Flux.never().doOnCancel(() -> cancelled.set(true)))
                .thenCancel().verify();
        assertTrue(cancelled.get());
    }

    @Test
    void usingReleasesResourceOnCompletionAndCancellation() {
        AtomicInteger closed = new AtomicInteger();
        StepVerifier.create(Mono.using(() -> "resource", Mono::just,
                        ignored -> closed.incrementAndGet(), true))
                .expectNext("resource").verifyComplete();
        StepVerifier.create(Flux.using(() -> "resource", ignored -> Flux.never(),
                        ignored -> closed.incrementAndGet()))
                .thenCancel().verify();
        assertEquals(2, closed.get());
    }

    @Test
    void cacheReusesSuccessfulSignals() {
        AtomicInteger calls = new AtomicInteger();
        Mono<Integer> cached = Mono.fromSupplier(calls::incrementAndGet).cache();
        StepVerifier.create(cached).expectNext(1).verifyComplete();
        StepVerifier.create(cached).expectNext(1).verifyComplete();
        assertEquals(1, calls.get());
    }

    @Test
    void cacheAlsoReplaysAnError() {
        AtomicInteger calls = new AtomicInteger();
        Mono<String> cached = Mono.<String>defer(() -> {
            calls.incrementAndGet();
            return Mono.error(new IOException("cached error"));
        }).cache();
        StepVerifier.create(cached).expectError(IOException.class).verify();
        StepVerifier.create(cached).expectError(IOException.class).verify();
        assertEquals(1, calls.get());
    }

    @Test
    void replaySinkKeepsOnlyItsConfiguredHistory() {
        Sinks.Many<String> sink = Sinks.many().replay().limit(2);
        sink.tryEmitNext("A").orThrow();
        sink.tryEmitNext("B").orThrow();
        sink.tryEmitNext("C").orThrow();
        sink.tryEmitComplete().orThrow();
        StepVerifier.create(sink.asFlux()).expectNext("B", "C").verifyComplete();
    }

    @Test
    void fallbackProbeProvesTheBranchWasSubscribed() {
        PublisherProbe<String> fallback = PublisherProbe.of(Mono.just("fallback"));
        StepVerifier.create(Mono.<String>empty().switchIfEmpty(fallback.mono()))
                .expectNext("fallback").verifyComplete();
        fallback.assertWasSubscribed();
        fallback.assertWasRequested();
        fallback.assertWasNotCancelled();
    }

    @Test
    void cacheHitDoesNotSubscribeToFallback() {
        PublisherProbe<String> database = PublisherProbe.of(Mono.just("db"));
        StepVerifier.create(Mono.just("cache").switchIfEmpty(database.mono()))
                .expectNext("cache").verifyComplete();
        database.assertWasNotSubscribed();
    }

    @Test
    void testPublisherControlsWhenUpstreamFails() {
        TestPublisher<String> source = TestPublisher.create();
        StepVerifier.create(source.flux().map(String::toUpperCase))
                .then(() -> source.next("a")).expectNext("A")
                .then(() -> source.error(new IOException("offline")))
                .expectError(IOException.class).verify();
    }

    @Test
    void bufferAndWindowHaveDifferentOutputTypes() {
        StepVerifier.create(Flux.range(1, 5).buffer(2))
                .expectNext(List.of(1, 2), List.of(3, 4), List.of(5)).verifyComplete();
        StepVerifier.create(Flux.range(1, 5).window(2).concatMap(Flux::count))
                .expectNext(2L, 2L, 1L).verifyComplete();
    }

    @Test
    void subscribeOnSchedulesTheCallable() {
        StepVerifier.create(Mono.fromCallable(() -> Thread.currentThread().getName())
                        .subscribeOn(Schedulers.boundedElastic()))
                .assertNext(name -> assertTrue(name.startsWith("boundedElastic-")))
                .verifyComplete();
    }

    @Test
    void createDisposesItsListenerOnCancellation() {
        AtomicBoolean unregistered = new AtomicBoolean();
        Flux<String> callbackBridge = Flux.create(sink ->
                sink.onDispose(() -> unregistered.set(true)));
        StepVerifier.create(callbackBridge).thenCancel().verify();
        assertTrue(unregistered.get());
    }
}
