package com.hospital.service;

import com.hospital.dto.SymptomAnalysis;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SymptomAnalyzerServiceTest {

    private static SymptomAnalyzerService analyzer;

    @BeforeAll
    static void trainOnce() {
        analyzer = new SymptomAnalyzerService();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "'I have chest pain and my heart is racing', Cardiology",
            "'Itchy red rash on my skin', Dermatology",
            "'Twisted my ankle, it is swollen and I cannot walk', Orthopedics",
            "'Bad migraine headache with numbness in my hand', Neurology",
            "'Sore throat and pain in my ear', ENT",
            "'Stomach pain with vomiting and diarrhea', Gastroenterology",
            "'Wheezing and coughing all night, asthma', Pulmonology",
            "'Fever and body ache for two days', General Medicine"
    })
    void suggestsExpectedDepartment(String symptoms, String expectedDepartment) {
        SymptomAnalysis result = analyzer.analyze(symptoms);

        assertThat(result.department()).isEqualTo(expectedDepartment);
        assertThat(result.confidence()).isBetween(0.0, 1.0);
        assertThat(result.scores()).hasSize(8).containsKey(expectedDepartment);
    }

    @Test
    void blankInputFallsBackToGeneralMedicine() {
        SymptomAnalysis result = analyzer.analyze("   ");

        assertThat(result.department()).isEqualTo(SymptomAnalyzerService.DEFAULT_DEPARTMENT);
        assertThat(result.confidence()).isZero();
    }
}
