// notifications\src\main\java\mp\config\kafka\KafkaProcessor.java

package mp.config.kafka;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface KafkaProcessor {

    String AUTHOR_REVIEW_IN = "author-review-in";
    String BOOK_PUBLISHED_IN = "book-published-in";

    @Input(AUTHOR_REVIEW_IN)
    SubscribableChannel authorReviewIn();

    @Input(BOOK_PUBLISHED_IN)
    SubscribableChannel bookPublishedIn();
}
