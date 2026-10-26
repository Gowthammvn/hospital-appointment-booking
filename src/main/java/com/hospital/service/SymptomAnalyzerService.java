package com.hospital.service;

import com.hospital.dto.SymptomAnalysis;
import opennlp.tools.doccat.*;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Suggests a hospital department from free-text symptoms using an Apache OpenNLP
 * document categorizer (maximum entropy, bag-of-words) trained at startup.
 */
@Service
public class SymptomAnalyzerService {

    public static final String DEFAULT_DEPARTMENT = "General Medicine";
    private static final String TRAINING_FILE = "nlp/symptom-training.txt";

    private static final Logger log = LoggerFactory.getLogger(SymptomAnalyzerService.class);

    private final DoccatModel model;

    public SymptomAnalyzerService() {
        this.model = train();
    }

    private static DoccatModel train() {
        ClassPathResource resource = new ClassPathResource(TRAINING_FILE);
        try (ObjectStream<String> lines = new PlainTextByLineStream(resource::getInputStream, StandardCharsets.UTF_8);
             ObjectStream<DocumentSample> samples = new DocumentSampleStream(lines)) {

            TrainingParameters params = TrainingParameters.defaultParams();
            params.put(TrainingParameters.CUTOFF_PARAM, 1);
            params.put(TrainingParameters.ITERATIONS_PARAM, 300);

            DoccatFactory factory = new DoccatFactory(new FeatureGenerator[]{new BagOfWordsFeatureGenerator()});
            DoccatModel trained = DocumentCategorizerME.train("en", samples, params, factory);
            log.info("Symptom categorizer trained from {}", TRAINING_FILE);
            return trained;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not train symptom categorizer", e);
        }
    }

    public SymptomAnalysis analyze(String symptoms) {
        String[] tokens = tokenize(symptoms);
        if (tokens.length == 0) {
            return new SymptomAnalysis(DEFAULT_DEPARTMENT, 0.0, Map.of());
        }

        // DocumentCategorizerME is not thread-safe; it is cheap to create per request.
        DocumentCategorizerME categorizer = new DocumentCategorizerME(model);
        double[] outcomes = categorizer.categorize(tokens);

        Map<String, Double> scores = new LinkedHashMap<>();
        int best = 0;
        for (int i = 0; i < outcomes.length; i++) {
            scores.put(toDepartmentName(categorizer.getCategory(i)), round(outcomes[i]));
            if (outcomes[i] > outcomes[best]) {
                best = i;
            }
        }
        return new SymptomAnalysis(toDepartmentName(categorizer.getCategory(best)), round(outcomes[best]), scores);
    }

    private static String[] tokenize(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(SimpleTokenizer.INSTANCE.tokenize(text.toLowerCase(Locale.ROOT)))
                .filter(t -> t.chars().anyMatch(Character::isLetter))
                .toArray(String[]::new);
    }

    /** Training categories use underscores instead of spaces (e.g. General_Medicine). */
    private static String toDepartmentName(String category) {
        return category.replace('_', ' ');
    }

    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
