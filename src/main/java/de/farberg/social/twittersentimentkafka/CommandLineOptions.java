package de.farberg.social.twittersentimentkafka;

import org.apache.log4j.Level;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class CommandLineOptions {

	@Option(name = "--kafka-bootstrap-server", usage = "Kafka bootstrap server", required = true)
	public String kafkaBootstrapServer = null;

	@Option(name = "--kafka-topic", usage = "Kafka topic to send data to", required = true)
	public String kafkaTopic = null;

	@Option(name = "--twitter-oauth-consumer-key", usage = "Twitter consumer key", required = true)
	public String twitterOAuthConsumerKey = null;

	@Option(name = "--twitter-oauth-consumer-secret", usage = "Twitter consumer secret", required = true)
	public String twitterOAuthConsumerSecret = null;

	@Option(name = "--twitter-oauth-access-token", usage = "Twitter access token", required = true)
	public String twitterOAuthAccessToken = null;

	@Option(name = "--twitter-oauth-access-token-secret", usage = "Twitter access token secret", required = true)
	public String twitterOAuthAccessTokenSecret = null;

	@Option(name = "--track-keywords", usage = "Comma-separated list of keywords w/o spaces", required = true)
	public String trackKeywords = null;

	@Option(name = "--verbose", usage = "Verbose (DEBUG) logging output (default: INFO).", required = false)
	public boolean verbose = false;

	@Option(name = "-h", aliases = { "--help" }, usage = "This help message.", required = false)
	public boolean help = false;

	static CommandLineOptions parseCmdLineOptions(final String[] args) {
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
			if (options.help)
				printHelpAndExit(parser);

			if (options.verbose) {
				org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
			}

		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			printHelpAndExit(parser);
		}

		return options;
	}

	private static void printHelpAndExit(CmdLineParser parser) {
		System.err.print("Usage: java " + Main.class.getCanonicalName());
		parser.printSingleLineUsage(System.err);
		System.err.println();
		parser.printUsage(System.err);
		System.exit(1);
	}
}