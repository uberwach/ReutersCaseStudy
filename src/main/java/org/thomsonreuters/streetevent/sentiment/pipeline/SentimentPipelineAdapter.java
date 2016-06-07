package org.thomsonreuters.streetevent.sentiment.pipeline;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.sentiment.SentimentUtils;
import edu.stanford.nlp.trees.MemoryTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;
import org.ejml.simple.SimpleMatrix;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.ejml.simple.SimpleMatrix;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.MemoryTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;

/**
 * Created by gerrit on 01.11.2014.
 * This is the adjusted SentimentPipeline provided by John Bauer from Stanford's Sentiment Analysis.
 * The problem with the class that it was only executable as a main. Now it is callable like a "normal" java class.
 * Also, I started to reduce its functionality in terms of input/output and parameter selection.
 */

/**
 * A wrapper class which creates a suitable pipeline for the sentiment
 * model and processes raw text.
 * <br>
 * The main program has the following options: <br>
 * <code>-parserModel</code> Which parser model to use, defaults to englishPCFG.ser.gz <br>
 * <code>-sentimentModel</code> Which sentiment model to use, defaults to sentiment.ser.gz <br>
 * <code>-file</code> Which file to process. <br>
 * <code>-fileList</code> A comma separated list of files to process. <br>
 * <code>-stdin</code> Read one line at a time from stdin. <br>
 * <code>-output</code> pennTrees: Output trees with scores at each binarized node.  vectors: Number tree nodes and print out the vectors.  probabilities: Output the scores for different labels for each node. Defaults to printing just the root. <br>
 * <code>-filterUnknown</code> remove unknown trees from the input.  Only applies to TREES input, in which case the trees must be binarized with sentiment labels <br>
 * <code>-help</code> Print out help <br>
 *
 * @author John Bauer
 */
public class SentimentPipelineAdapter {
    private static final NumberFormat NF = new DecimalFormat("0.0000");

    static enum Output {
        PENNTREES, VECTORS, ROOT, PROBABILITIES
    }

    static enum Input {
        TEXT
    }

    /**
     * Sets the labels on the tree (except the leaves) to be the integer
     * value of the sentiment prediction.  Makes it easy to print out
     * with Tree.toString()
     */
    static void setSentimentLabels(Tree tree) {
        if (tree.isLeaf()) {
            return;
        }

        for (Tree child : tree.children()) {
            setSentimentLabels(child);
        }

        Label label = tree.label();
        if (!(label instanceof CoreLabel)) {
            throw new IllegalArgumentException("Required a tree with CoreLabels");
        }
        CoreLabel cl = (CoreLabel) label;
        cl.setValue(Integer.toString(RNNCoreAnnotations.getPredictedClass(tree)));
    }

    /**
     * Sets the labels on the tree to be the indices of the nodes.
     * Starts counting at the root and does a postorder traversal.
     */
    static int setIndexLabels(Tree tree, int index) {
        if (tree.isLeaf()) {
            return index;
        }

        tree.label().setValue(Integer.toString(index));
        index++;
        for (Tree child : tree.children()) {
            index = setIndexLabels(child, index);
        }
        return index;
    }

    /**
     * Outputs the vectors from the tree.  Counts the tree nodes the
     * same as setIndexLabels.
     */
    static int outputTreeVectors(PrintStream out, Tree tree, int index) {
        if (tree.isLeaf()) {
            return index;
        }

        out.print("  " + index + ":");
        SimpleMatrix vector = RNNCoreAnnotations.getNodeVector(tree);
        for (int i = 0; i < vector.getNumElements(); ++i) {
            out.print("  " + NF.format(vector.get(i)));
        }
        out.println();
        index++;
        for (Tree child : tree.children()) {
            index = outputTreeVectors(out, child, index);
        }
        return index;
    }

    /**
     * Outputs the scores from the tree.  Counts the tree nodes the
     * same as setIndexLabels.
     */
    static int outputTreeScores(PrintStream out, Tree tree, int index) {
        if (tree.isLeaf()) {
            return index;
        }

        out.print("  " + index + ":");
        SimpleMatrix vector = RNNCoreAnnotations.getPredictions(tree);
        for (int i = 0; i < vector.getNumElements(); ++i) {
            out.print("  " + NF.format(vector.get(i)));
        }
        out.println();
        index++;
        for (Tree child : tree.children()) {
            index = outputTreeScores(out, child, index);
        }
        return index;
    }

    /**
     * Outputs a tree using the output style requested
     */
    static void outputTree(PrintStream out, CoreMap sentence, List<Output> outputFormats) {
        Tree tree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
        for (Output output : outputFormats) {
            switch (output) {
                case PENNTREES: {
                    Tree copy = tree.deepCopy();
                    setSentimentLabels(copy);
                    out.println(copy);
                    break;
                }
                case VECTORS: {
                    Tree copy = tree.deepCopy();
                    setIndexLabels(copy, 0);
                    out.println(copy);
                    outputTreeVectors(out, tree, 0);
                    break;
                }
                case ROOT: {
                    out.println("  " + sentence.get(SentimentCoreAnnotations.ClassName.class));
                    break;
                }
                case PROBABILITIES: {
                    Tree copy = tree.deepCopy();
                    setIndexLabels(copy, 0);
                    out.println(copy);
                    outputTreeScores(out, tree, 0);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown output format " + output);
            }
        }
    }

    static final String DEFAULT_TLPP_CLASS = "edu.stanford.nlp.parser.lexparser.EnglishTreebankParserParams";

    public static void help() {
        System.err.println("Known command line arguments:");
        System.err.println("  -sentimentModel <model>: Which model to use");
        System.err.println("  -parserModel <model>: Which parser to use");
        System.err.println("  -file <filename>: Which file to process");
        System.err.println("  -fileList <file>,<file>,...: Comma separated list of files to process.  Output goes to file.out");
        System.err.println("  -stdin: Process stdin instead of a file");
        System.err.println("  -input <format>: Which format to input, TEXT or TREES.  Will not process stdin as trees.  If trees are not already binarized, they will be binarized with -tlppClass's headfinder, which means they must have labels in that treebank's tagset.");
        System.err.println("  -output <format>: Which format to output, PENNTREES, VECTORS, PROBABILITIES, or ROOT.  Multiple formats can be specified as a comma separated list.");
        System.err.println("  -filterUnknown: remove unknown trees from the input.  Only applies to TREES input, in which case the trees must be binarized with sentiment labels");
        System.err.println("  -tlppClass: a class to use for building the binarizer if using non-binarized TREES as input.  Defaults to " + DEFAULT_TLPP_CLASS);
    }

    /**
     * Reads an annotation from the given filename using the requested input.
     */
    public static List<Annotation> getAnnotations(StanfordCoreNLP tokenizer, Input inputFormat, String text, boolean filterUnknown) {
        switch (inputFormat) {
            case TEXT: {
                Annotation annotation = new Annotation(text);
                tokenizer.annotate(annotation);
                List<Annotation> annotations = Generics.newArrayList();
                for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                    Annotation nextAnnotation = new Annotation(sentence.get(CoreAnnotations.TextAnnotation.class));
                    nextAnnotation.set(CoreAnnotations.SentencesAnnotation.class, Collections.singletonList(sentence));
                    annotations.add(nextAnnotation);
                }
                return annotations;
            }
            default:
                throw new IllegalArgumentException("Unknown format " + inputFormat);
        }
    }

    public static List<Annotation> parse(String text) throws IOException {
        String parserModel = null;
        String sentimentModel = null;

        boolean filterUnknown = false;

        List<Output> outputFormats = Arrays.asList(new Output[]{Output.VECTORS});
        Input inputFormat = Input.TEXT;
        String tlppClass = DEFAULT_TLPP_CLASS;

        // We construct two pipelines.  One handles tokenization, if
        // necessary.  The other takes tokenized sentences and converts
        // them to sentiment trees.
        Properties pipelineProps = new Properties();
        Properties tokenizerProps = null;
        if (sentimentModel != null) {
            pipelineProps.setProperty("sentiment.model", sentimentModel);
        }
        if (parserModel != null) {
            pipelineProps.setProperty("parse.model", parserModel);
        }


        pipelineProps.setProperty("annotators", "parse, sentiment");
        pipelineProps.setProperty("enforceRequirements", "false");
        tokenizerProps = new Properties();
        tokenizerProps.setProperty("annotators", "tokenize, ssplit");


        StanfordCoreNLP tokenizer = (tokenizerProps == null) ? null : new StanfordCoreNLP(tokenizerProps);
        StanfordCoreNLP pipeline = new StanfordCoreNLP(pipelineProps);


        // Process a file.  The pipeline will do tokenization, which
        // means it will split it into sentences as best as possible
        // with the tokenizer.
        List<Annotation> annotations = getAnnotations(tokenizer, inputFormat, text, filterUnknown);
        for (Annotation annotation : annotations) {
            pipeline.annotate(annotation);
            // for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            //    System.out.println(sentence);
            //    outputTree(System.out, sentence, outputFormats);
            //}
        }

        return annotations;
    }
}
