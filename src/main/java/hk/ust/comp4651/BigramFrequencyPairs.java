package hk.ust.comp4651;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

/**
 * Compute the bigram count using "pairs" approach
 */
public class BigramFrequencyPairs extends Configured implements Tool {
	private static final Logger LOG = Logger.getLogger(BigramFrequencyPairs.class);

	/*
	 * TODO: write your Mapper here
	 */
	private static class MyMapper extends
			Mapper<LongWritable, Text, PairOfStrings, IntWritable> {

		// Reuse objects to save overhead of object creation.
		private static final IntWritable ONE = new IntWritable(1);
		private static final PairOfStrings BIGRAM = new PairOfStrings();

		@Override
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] words = line.trim().split("\\s+");
			
			/*
			 * TODO: Your implementation goes here.
			 */
			if (words.length > 1) {
				for (int i = 0; i+1 < words.length; i++) {
					String currWord = words[i];
					String nextWord = words[i+1];
					if (currWord.isEmpty()) {
						continue;
					}

					BIGRAM.set(currWord, nextWord);
					context.write(BIGRAM, ONE);

					BIGRAM.set(currWord, "*");
					context.write(BIGRAM, ONE);
				}
			}
		}
	}

	/*
	 * TODO: Write your reducer here
	 */
	private static class MyReducer extends
			Reducer<PairOfStrings, IntWritable, PairOfStrings, FloatWritable> {

		// Reuse objects.
		private final static FloatWritable RELATIVE_FREQUENCY = new FloatWritable();
		private final static LongWritable MARGINAL = new LongWritable();
		private String currentWord = null;

		@Override
		public void reduce(PairOfStrings key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			/*
			 * TODO: Your implementation goes here.
			 */

			if (currentWord == null || key.getLeftElement().equals(currentWord)) {
				currentWord = key.getRightElement();
				MARGINAL.set(0L);
			}

			long sum = 0L;
			for (IntWritable val : values) {
				sum += val.get();
			}

			if (key.getRightElement().equals("*")) {
				// Store marginal for this word
				MARGINAL.set(sum);
				context.write(new PairOfStrings(key.getLeftElement(), ""), new FloatWritable(sum));
			} else {
				if (MARGINAL.get() == 0L) {
					System.out.println("ERROR, no marginal found meaning some entries are in order before '*' which should not happen!");
					return;
				}
				float relativeFrequency = (float) ((double) sum / (double) MARGINAL.get());
				RELATIVE_FREQUENCY.set(relativeFrequency);
				context.write(key, RELATIVE_FREQUENCY);
			}
		}
	}
	
	private static class MyCombiner extends
			Reducer<PairOfStrings, IntWritable, PairOfStrings, IntWritable> {
		private static final IntWritable SUM = new IntWritable();

		@Override
		public void reduce(PairOfStrings key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			/*
			 * TODO: Your implementation goes here.
			 */
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			SUM.set(sum);
			context.write(key, SUM);
		}
	}

	/*
	 * Partition bigrams based on their left elements
	 */
	private static class MyPartitioner extends
			Partitioner<PairOfStrings, IntWritable> {
		@Override
		public int getPartition(PairOfStrings key, IntWritable value,
				int numReduceTasks) {
			return (key.getLeftElement().hashCode() & Integer.MAX_VALUE)
					% numReduceTasks;
		}
	}

	public static class StarFirstComparator extends WritableComparator {
		private static final Text.Comparator TEXT_COMPARATOR = new Text.Comparator();

		public StarFirstComparator() {
			super(PairOfStrings.class);
		}

		@Override
		public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
			try {
				int firstL1 = WritableUtils.decodeVIntSize(b1[s1]) + readVInt(b1, s1);
				int firstL2 = WritableUtils.decodeVIntSize(b2[s2]) + readVInt(b2, s2);

				// compare left
				int cmp = TEXT_COMPARATOR.compare(b1, s1, firstL1, b2, s2, firstL2);
				if (cmp != 0) return cmp;

				// when same left, compare right. Enforces "*" to come first
				int r1s = s1 + firstL1, r1l = l1 - firstL1;
				int r2s = s2 + firstL2, r2l = l2 - firstL2;

				// check if right == "*"
				boolean s1star = (r1l == 2 && b1[r1s] == 1 && b1[r1s + 1] == (byte) '*');
				boolean s2star = (r2l == 2 && b2[r2s] == 1 && b2[r2s + 1] == (byte) '*');

				if (s1star && !s2star) return -1;
				if (!s1star && s2star) return 1;

				return TEXT_COMPARATOR.compare(b1, r1s, r1l, b2, r2s, r2l);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	/**
	 * Creates an instance of this tool.
	 */
	public BigramFrequencyPairs() {
	}

	private static final String INPUT = "input";
	private static final String OUTPUT = "output";
	private static final String NUM_REDUCERS = "numReducers";

	/**
	 * Runs this tool.
	 */
	@SuppressWarnings({ "static-access" })
	public int run(String[] args) throws Exception {
		Options options = new Options();

		options.addOption(OptionBuilder.withArgName("path").hasArg()
				.withDescription("input path").create(INPUT));
		options.addOption(OptionBuilder.withArgName("path").hasArg()
				.withDescription("output path").create(OUTPUT));
		options.addOption(OptionBuilder.withArgName("num").hasArg()
				.withDescription("number of reducers").create(NUM_REDUCERS));

		CommandLine cmdline;
		CommandLineParser parser = new GnuParser();

		try {
			cmdline = parser.parse(options, args);
		} catch (ParseException exp) {
			System.err.println("Error parsing command line: "
					+ exp.getMessage());
			return -1;
		}

		// Lack of arguments
		if (!cmdline.hasOption(INPUT) || !cmdline.hasOption(OUTPUT)) {
			System.out.println("args: " + Arrays.toString(args));
			HelpFormatter formatter = new HelpFormatter();
			formatter.setWidth(120);
			formatter.printHelp(this.getClass().getName(), options);
			ToolRunner.printGenericCommandUsage(System.out);
			return -1;
		}

		String inputPath = cmdline.getOptionValue(INPUT);
		String outputPath = cmdline.getOptionValue(OUTPUT);
		int reduceTasks = cmdline.hasOption(NUM_REDUCERS) ? Integer
				.parseInt(cmdline.getOptionValue(NUM_REDUCERS)) : 1;

		LOG.info("Tool: " + BigramFrequencyPairs.class.getSimpleName());
		LOG.info(" - input path: " + inputPath);
		LOG.info(" - output path: " + outputPath);
		LOG.info(" - number of reducers: " + reduceTasks);

		// Create and configure a MapReduce job
		Configuration conf = getConf();
		Job job = Job.getInstance(conf);
		job.setJobName(BigramFrequencyPairs.class.getSimpleName());
		job.setJarByClass(BigramFrequencyPairs.class);

		job.setNumReduceTasks(reduceTasks);

		FileInputFormat.setInputPaths(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		job.setMapOutputKeyClass(PairOfStrings.class);
		job.setMapOutputValueClass(IntWritable.class);
		job.setOutputKeyClass(PairOfStrings.class);
		job.setOutputValueClass(FloatWritable.class);

		/*
		 * A MapReduce program consists of three components: a mapper, a
		 * reducer, a combiner (which reduces the amount of shuffle data), and a partitioner
		 */
		job.setMapperClass(MyMapper.class);
		job.setCombinerClass(MyCombiner.class);
		job.setSortComparatorClass(StarFirstComparator.class);
		job.setPartitionerClass(MyPartitioner.class);
		job.setReducerClass(MyReducer.class);

		// Delete the output directory if it exists already.
		Path outputDir = new Path(outputPath);
		FileSystem.get(conf).delete(outputDir, true);

		// Time the program
		long startTime = System.currentTimeMillis();
		job.waitForCompletion(true);
		LOG.info("Job Finished in " + (System.currentTimeMillis() - startTime)
				/ 1000.0 + " seconds");

		return 0;
	}

	/**
	 * Dispatches command-line arguments to the tool via the {@code ToolRunner}.
	 */
	public static void main(String[] args) throws Exception {
		ToolRunner.run(new BigramFrequencyPairs(), args);
	}
}
