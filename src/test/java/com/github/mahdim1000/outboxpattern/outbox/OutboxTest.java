package com.github.mahdim1000.outboxpattern.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Outbox Entity Tests")
class OutboxTest {

    private static final String TEST_TOPIC = "test-topic";
    private static final String TEST_AGGREGATE_ID = "123";
    private static final String TEST_PAYLOAD = "{\"message\": \"test payload\"}";
    private static final String ERROR_MESSAGE = "Failed to publish message";

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        void shouldCreateOutboxWithValidTopicAndPayload() {
            Outbox outbox = Outbox.create(TEST_TOPIC, TEST_AGGREGATE_ID, TEST_PAYLOAD);

            assertThat(outbox).isNotNull();
            assertThat(outbox.getId()).isNotNull().isNotEmpty();
            assertThat(outbox.getTopic()).isEqualTo(TEST_TOPIC);
            assertThat(outbox.getAggregateId()).isEqualTo(TEST_AGGREGATE_ID);
            assertThat(outbox.getPayload()).isEqualTo(TEST_PAYLOAD);
            assertThat(outbox.getVersion()).isEqualTo(1);
            assertThat(outbox.getStatus()).isEqualTo(Outbox.Status.PENDING);
            assertThat(outbox.getCreatedAt()).isNotNull();
            assertThat(outbox.getRetryCount()).isZero();
            assertThat(outbox.getErrorMessage()).isNull();
            assertThat(outbox.getRetryAt()).isNull();
            assertThat(outbox.getPublishedAt()).isNull();
        }

        @Test
        void shouldCreateOutboxWithUniqueIds() {
            Outbox outbox1 = Outbox.create(TEST_TOPIC, TEST_AGGREGATE_ID, TEST_PAYLOAD);
            Outbox outbox2 = Outbox.create(TEST_TOPIC, TEST_AGGREGATE_ID, TEST_PAYLOAD);

            assertThat(outbox1.getId()).isNotEqualTo(outbox2.getId());
        }

        @Test
        void shouldSetNextRetryAtToCurrentTimeOnCreation() {
            LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
            Outbox outbox = Outbox.create(TEST_TOPIC, TEST_AGGREGATE_ID, TEST_PAYLOAD);
            LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);

            assertThat(outbox.getCreatedAt()).isBetween(beforeCreation, afterCreation);
        }

        @Test
        void shouldThrowExceptionOnNullTopic() {
            try {
                Outbox.create(null, TEST_AGGREGATE_ID, TEST_PAYLOAD);
            } catch (Exception e) {
                assertThat(e).isInstanceOf(IllegalArgumentException.class);
            }
        }

        @Test
        void shouldThrowExceptionOnNullPayload() {
            try {
                Outbox.create(TEST_TOPIC, TEST_AGGREGATE_ID, null);
            } catch (Exception e) {
                assertThat(e).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Nested
    class PublishFailureHandlingTests {

        private Outbox outbox;

        @BeforeEach
        void setUp() {
            outbox = Outbox.create(TEST_TOPIC, TEST_AGGREGATE_ID, TEST_PAYLOAD);
        }

        @Test
        void shouldIncrementRetryCountOnFirstFailure() {
            outbox.handlePublishFailure(ERROR_MESSAGE);

            assertThat(outbox.getRetryCount()).isEqualTo(1);
            assertThat(outbox.getErrorMessage()).isEqualTo(ERROR_MESSAGE);
            assertThat(outbox.getRetryAt()).isNotNull();
            assertThat(outbox.getStatus()).isEqualTo(Outbox.Status.PENDING);
        }

        @Test
        void shouldIncrementRetryCountOnSubsequentFailures() {
            outbox.handlePublishFailure(ERROR_MESSAGE);
            outbox.handlePublishFailure(ERROR_MESSAGE);
            outbox.handlePublishFailure(ERROR_MESSAGE);

            assertThat(outbox.getRetryCount()).isEqualTo(3);
            assertThat(outbox.getStatus()).isEqualTo(Outbox.Status.PENDING);
        }

        @Test
        void shouldSetStatusToFailedAfterMaxRetriesExceeded() {
            outbox.handlePublishFailure(ERROR_MESSAGE);
            outbox.handlePublishFailure(ERROR_MESSAGE);
            outbox.handlePublishFailure(ERROR_MESSAGE);
            outbox.handlePublishFailure(ERROR_MESSAGE);

            assertThat(outbox.getRetryCount()).isEqualTo(4);
            assertThat(outbox.getStatus()).isEqualTo(Outbox.Status.FAILED);
            assertThat(outbox.getErrorMessage()).isEqualTo(ERROR_MESSAGE);
        }

        @Test
        void shouldUpdateRetryTimestampOnEachFailure() {
            LocalDateTime beforeFailure = LocalDateTime.now().minusSeconds(1);
            outbox.handlePublishFailure(ERROR_MESSAGE);
            LocalDateTime afterFailure = LocalDateTime.now().plusSeconds(1);

            assertThat(outbox.getRetryAt()).isBetween(beforeFailure, afterFailure);
        }

        @Test
        void shouldHandleNullErrorMessageGracefully() {
            assertThatCode(() -> outbox.handlePublishFailure(null))
                    .doesNotThrowAnyException();

            assertThat(outbox.getErrorMessage()).isNull();
            assertThat(outbox.getRetryCount()).isEqualTo(1);
        }

    }

    @Nested
    class PublishSuccessHandlingTests {

        private Outbox outbox;

        @BeforeEach
        void setUp() {
            outbox = Outbox.create(TEST_TOPIC, TEST_AGGREGATE_ID, TEST_PAYLOAD);
        }

        @Test
        void shouldSetStatusToPublishedOnSuccess() {
            outbox.handlePublishSuccess();

            assertThat(outbox.getStatus()).isEqualTo(Outbox.Status.PUBLISHED);
            assertThat(outbox.getPublishedAt()).isNotNull();
        }

        @Test
        void shouldSetPublishedTimestampOnSuccess() {
            LocalDateTime beforeSuccess = LocalDateTime.now().minusSeconds(1);
            outbox.handlePublishSuccess();
            LocalDateTime afterSuccess = LocalDateTime.now().plusSeconds(1);

            assertThat(outbox.getPublishedAt()).isBetween(beforeSuccess, afterSuccess);
        }

        @Test
        void shouldHandleSuccessAfterPreviousFailures() {
            outbox.handlePublishFailure(ERROR_MESSAGE);
            outbox.handlePublishFailure(ERROR_MESSAGE);

            outbox.handlePublishSuccess();

            assertThat(outbox.getStatus()).isEqualTo(Outbox.Status.PUBLISHED);
            assertThat(outbox.getPublishedAt()).isNotNull();
            assertThat(outbox.getRetryCount()).isEqualTo(2);
            assertThat(outbox.getErrorMessage()).isEqualTo(ERROR_MESSAGE);
        }
    }
}
