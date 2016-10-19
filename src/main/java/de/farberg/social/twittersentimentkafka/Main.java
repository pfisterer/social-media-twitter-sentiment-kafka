package de.farberg.social.twittersentimentkafka;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.util.logging.LogLevel;
import de.uniluebeck.itm.util.logging.Logging;
import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class Main {

	static {
		Logging.setLoggingDefaults(LogLevel.INFO, "[%-5p; %c{1}::%M] %m%n");
	}

	public static void main(String[] args) throws InterruptedException {
		// Obtain an instance of a logger for this class
		Logger log = LoggerFactory.getLogger(Main.class);
		CommandLineOptions options = CommandLineOptions.parseCmdLineOptions(args);

		// Convert comma-separated list of keywords to list with lower-case entries
		List<String> keywords = Arrays.stream(options.trackKeywords.split(","))
				.map(word -> word.trim().toLowerCase())
				.collect(Collectors.toList());

		// Setup Kafka
		Properties props = new Properties();
		props.put("bootstrap.servers", options.kafkaBootstrapServer);
		props.put("group.id", "bla");
		props.put("client.id", Main.class.getSimpleName());
		props.put("key.serializer", StringSerializer.class.getName());
		props.put("value.serializer", StringSerializer.class.getName());

		KafkaProducer<String, String> kafkaProducer = new KafkaProducer<String, String>(props);

		// Create sentiment analyzer instance
		SentimentAnalysis sentimentAnalysis = new SentimentAnalysis();

		// Create Twitter instance
		ConfigurationBuilder cb = new ConfigurationBuilder();

		cb.setDebugEnabled(options.verbose);
		cb.setOAuthConsumerKey(options.twitterOAuthConsumerKey);
		cb.setOAuthConsumerSecret(options.twitterOAuthConsumerSecret);
		cb.setOAuthAccessToken(options.twitterOAuthAccessToken);
		cb.setOAuthAccessTokenSecret(options.twitterOAuthAccessTokenSecret);

		TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();

		// Set up a listener that streams analyzed tweets to Kafka
		twitterStream.addListener(new StatusListener() {

			public void onStatus(Status status) {
				Sentiment sentimentValue = sentimentAnalysis.getSentimentValue(status.getText());

				log.debug("Received {} message: {}", sentimentValue, status.getText());

				// Determine the keyword
				// TODO currently only the first keyword is used and the rest ignored -> improve this
				Optional<String> tweetKeyword = keywords.stream()
						.filter(word -> status.getText().toLowerCase().contains((word)))
						.findFirst();

				if (!tweetKeyword.isPresent())
					return;

				JsonObject jsonObject = Json.createObjectBuilder()
						.add("id", status.getId())
						.add("userId", status.getUser().getId())
						.add("messageText", status.getText())
						.add("likeCount", status.getFavoriteCount())
						.add("sentiment", sentimentValue.toString())
						.add("keyword", tweetKeyword.get())
						.build();

				kafkaProducer.send(new ProducerRecord<String, String>(options.kafkaTopic, jsonObject.toString()),
						(RecordMetadata metadata, Exception exception) -> {

							// Display some data about the message transmission
							if (metadata != null) {
								log.info("Sent " + sentimentValue + " / " + tweetKeyword.get() + " message (partition: "
										+ metadata.partition() + ", offset: " + metadata.offset() + "): " + jsonObject.toString());
							} else {
								log.warn("" + exception, exception);
							}

						});

			}

			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
				log.info("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
			}

			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
				log.info("Got track limitation notice:" + numberOfLimitedStatuses);
			}

			public void onScrubGeo(long userId, long upToStatusId) {
				log.info("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
			}

			public void onException(Exception e) {
				log.info("Exception received: " + e, e);
			}

			@Override
			public void onStallWarning(StallWarning warning) {
				log.info("Stall warning received: " + warning, warning);
			}
		});

		// Set a filter on the Twitter stream
		FilterQuery filter = new FilterQuery();
		filter.track(keywords.toArray(new String[0]));
		filter.language(new String[] { "en" });
		log.info("Using track filter: {}", filter);

		// Start sampling
		twitterStream.filter(filter);

		// Cleanup on shutdown
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				kafkaProducer.close();
			}
		});

		// Run forever
		while (true)
			Thread.sleep(1000);
	}
}
