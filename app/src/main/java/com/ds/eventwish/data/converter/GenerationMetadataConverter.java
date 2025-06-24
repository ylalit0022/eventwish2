package com.ds.eventwish.data.converter;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.Map;

/**
 * Type converter for GenerationMetadata objects in Room database
 * Handles conversion between JSON string and GenerationMetadata object
 */
public class GenerationMetadataConverter {
    private static final Gson gson = new Gson();

    /**
     * Convert GenerationMetadata object to JSON string for storage
     */
    @TypeConverter
    public static String fromGenerationMetadata(GenerationMetadata metadata) {
        return metadata == null ? null : gson.toJson(metadata);
    }

    /**
     * Convert JSON string to GenerationMetadata object
     */
    @TypeConverter
    public static GenerationMetadata toGenerationMetadata(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(value, GenerationMetadata.class);
        } catch (JsonSyntaxException e) {
            // Return null if JSON is invalid
            return null;
        }
    }

    /**
     * GenerationMetadata data class for AI generation information
     */
    public static class GenerationMetadata {
        private String provider;
        private String trainingData;
        private String bias;
        private String limitations;
        private String capabilities;
        private float quality;
        private long processingTime;
        private float confidence;
        private String version;
        private Map<String, Object> parameters;

        // Default constructor
        public GenerationMetadata() {}

        // Constructor with basic parameters
        public GenerationMetadata(String provider, float quality, long processingTime, float confidence) {
            this.provider = provider;
            this.quality = quality;
            this.processingTime = processingTime;
            this.confidence = confidence;
        }

        // Getters
        public String getProvider() { return provider; }
        public String getTrainingData() { return trainingData; }
        public String getBias() { return bias; }
        public String getLimitations() { return limitations; }
        public String getCapabilities() { return capabilities; }
        public float getQuality() { return quality; }
        public long getProcessingTime() { return processingTime; }
        public float getConfidence() { return confidence; }
        public String getVersion() { return version; }
        public Map<String, Object> getParameters() { return parameters; }

        // Setters
        public void setProvider(String provider) { this.provider = provider; }
        public void setTrainingData(String trainingData) { this.trainingData = trainingData; }
        public void setBias(String bias) { this.bias = bias; }
        public void setLimitations(String limitations) { this.limitations = limitations; }
        public void setCapabilities(String capabilities) { this.capabilities = capabilities; }
        public void setQuality(float quality) { this.quality = quality; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
        public void setVersion(String version) { this.version = version; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

        @Override
        public String toString() {
            return "GenerationMetadata{" +
                    "provider='" + provider + '\'' +
                    ", quality=" + quality +
                    ", processingTime=" + processingTime +
                    ", confidence=" + confidence +
                    ", version='" + version + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            GenerationMetadata that = (GenerationMetadata) obj;
            return Float.compare(that.quality, quality) == 0 &&
                    processingTime == that.processingTime &&
                    Float.compare(that.confidence, confidence) == 0 &&
                    java.util.Objects.equals(provider, that.provider) &&
                    java.util.Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(provider, quality, processingTime, confidence, version);
        }
    }
} 