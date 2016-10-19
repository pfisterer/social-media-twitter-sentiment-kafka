package de.farberg.social.twittersentimentkafka;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class SentimentAnalysis {

	private Logger log = LoggerFactory.getLogger(SentimentAnalysis.class);

	private StanfordCoreNLP pipeline;

	public SentimentAnalysis() {
		Properties properties = new Properties();
		properties.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
		pipeline = new StanfordCoreNLP(properties);
	}

	public Sentiment getSentimentValue(String text) {
		int finalSentimentValue = 0;
		int longestTextPart = 0;

		if (text == null || text.length() <= 0) {
			log.warn("Unable to process null or empty string: {}", text);
			return Sentiment.NONE_DETECTED;
		}

		Annotation annotation = pipeline.process(text);

		for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
			Tree tree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
			int sentiment = RNNCoreAnnotations.getPredictedClass(tree);

			String partText = sentence.toString();
			log.trace("Looking at part of text: {}", partText);

			if (partText.length() > longestTextPart) {
				finalSentimentValue = sentiment;
				longestTextPart = partText.length();
			}

		}

		Sentiment sentimentValue = Sentiment.NEUTRAL;

		if (finalSentimentValue == 1) {
			// TODO This seems to be more a neutral statement
			sentimentValue = Sentiment.NEGATIVE;
		} else if (finalSentimentValue == 3) {
			sentimentValue = Sentiment.HAPPY;
		} else if (finalSentimentValue == 4) {
			sentimentValue = Sentiment.EUPHORIC;
		}

		log.trace("Sentiment is {} for: {}", finalSentimentValue, text);
		return sentimentValue;
	}
}