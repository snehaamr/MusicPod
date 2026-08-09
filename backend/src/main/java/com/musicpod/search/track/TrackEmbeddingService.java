package com.musicpod.search.track;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TrackEmbeddingService {

    private final ObjectProvider<EmbeddingModel>
            embeddingModelProvider;

    private final int expectedDimensions;

    private final String model;

    public TrackEmbeddingService(
            ObjectProvider<EmbeddingModel>
                    embeddingModelProvider,

            @Value(
                    "${spring.ai.openai.embedding.dimensions:1024}"
            )
            int expectedDimensions,

            @Value(
                    "${spring.ai.openai.embedding.model:text-embedding-3-small}"
            )
            String model) {

        this.embeddingModelProvider =
                embeddingModelProvider;

        this.expectedDimensions =
                expectedDimensions;

        this.model =
                model;
    }

    public List<Float> embed(
            String text) {

        EmbeddingResponse response =
                embedRequest(
                        List.of(text)
                );

        if (response.getResults().isEmpty()) {

            throw new IllegalStateException(
                    "Embedding model returned no results"
            );
        }

        return toList(
                response
                        .getResult()
                        .getOutput()
        );
    }

    public List<List<Float>> embedAll(
            List<String> texts) {

        if (texts.isEmpty()) {
            return List.of();
        }

        EmbeddingResponse response =
                embedRequest(texts);

        List<Embedding> embeddings =
                new ArrayList<>(
                        response.getResults()
                );

        if (embeddings.size()
                != texts.size()) {

            throw new IllegalStateException(
                    "Expected "
                            + texts.size()
                            + " embeddings but received "
                            + embeddings.size()
            );
        }

        /*
         * OpenAI returns an index for each embedding.
         * Sort defensively so our vectors remain aligned
         * with the original input text order.
         */
        embeddings.sort(
                Comparator.comparingInt(
                        embedding ->
                                embedding.getIndex() == null
                                        ? Integer.MAX_VALUE
                                        : embedding.getIndex()
                )
        );

        List<List<Float>> result =
                new ArrayList<>(
                        embeddings.size()
                );

        for (Embedding embedding : embeddings) {

            result.add(
                    toList(
                            embedding.getOutput()
                    )
            );
        }

        return result;
    }

    private EmbeddingResponse embedRequest(
            List<String> texts) {

        OpenAiEmbeddingOptions options =
                OpenAiEmbeddingOptions
                        .builder()
                        .model(model)
                        .dimensions(
                                expectedDimensions
                        )
                        .build();

        EmbeddingRequest request =
                new EmbeddingRequest(
                        texts,
                        options
                );

        return requiredModel()
                .call(request);
    }

    private EmbeddingModel requiredModel() {

        EmbeddingModel embeddingModel =
                embeddingModelProvider
                        .getIfAvailable();

        if (embeddingModel == null) {

            throw new IllegalStateException(
                    "No EmbeddingModel is configured"
            );
        }

        return embeddingModel;
    }

    private List<Float> toList(
            float[] vector) {

        if (vector == null) {

            throw new IllegalStateException(
                    "Embedding model returned a null vector"
            );
        }

        if (vector.length
                != expectedDimensions) {

            throw new IllegalStateException(
                    "Embedding dimension mismatch. "
                            + "Expected "
                            + expectedDimensions
                            + " but received "
                            + vector.length
            );
        }

        List<Float> result =
                new ArrayList<>(
                        vector.length
                );

        for (float value : vector) {

            result.add(value);
        }

        return result;
    }
}