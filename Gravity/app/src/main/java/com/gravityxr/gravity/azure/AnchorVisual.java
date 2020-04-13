// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.
package com.gravityxr.gravity.azure;

import android.content.Context;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import com.gravityxr.gravity.MainThreadContext;
import com.gravityxr.gravity.R;
import com.microsoft.azure.spatialanchors.CloudSpatialAnchor;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class AnchorVisual {
    private final ArFragment arFragment;
    private String progressMessage;

    public void setLoadingText(@NotNull String progressMessage) {
        this.progressMessage = progressMessage;
    }

    public enum Shape {
        Sphere,
        Cube,
        Cylinder,
        CardPicker,
        AnchorRegister

    }

//    public enum Products {
//        Tv(R.layout.tv),
//        Chair(R.layout.chair),
//        Plant(R.layout.plant),
//        Sofa(R.layout.sofa),
//        Misc(R.layout.misc)
//    }

    private final AnchorNode anchorNode;
    private TransformableNode transformableNode;
    private CloudSpatialAnchor cloudAnchor;
    private Shape shape = Shape.CardPicker;
    private Material material;

    private static HashMap<Integer, CompletableFuture<Material>> solidColorMaterialCache = new HashMap<>();

    public AnchorVisual(ArFragment arFragment, Anchor localAnchor) {
        anchorNode = new AnchorNode(localAnchor);
        this.arFragment = arFragment;
        transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        transformableNode.getScaleController().setEnabled(false);
        transformableNode.getTranslationController().setEnabled(false);
        transformableNode.getRotationController().setEnabled(false);
        transformableNode.setParent(this.anchorNode);
    }

    public AnchorVisual(ArFragment arFragment, CloudSpatialAnchor cloudAnchor) {
        this(arFragment, cloudAnchor.getLocalAnchor());
        setCloudAnchor(cloudAnchor);
    }

    public AnchorNode getAnchorNode() {
        return this.anchorNode;
    }

    public CloudSpatialAnchor getCloudAnchor() {
        return this.cloudAnchor;
    }

    public Anchor getLocalAnchor() {
        return this.anchorNode.getAnchor();
    }

    public void render(ArFragment arFragment) {
        MainThreadContext.runOnUiThread(() -> {
            recreateRenderableOnUiThread();
            anchorNode.setParent(arFragment.getArSceneView().getScene());
        });
    }

    public void setCloudAnchor(CloudSpatialAnchor cloudAnchor) {
        this.cloudAnchor = cloudAnchor;
    }

    public synchronized void setColor(Context context, int rgb) {
        CompletableFuture<Material> loadMaterial =
                solidColorMaterialCache.computeIfAbsent(rgb,
                    color ->
                    {
                        CompletableFuture<Material> promise = new CompletableFuture<>();
                        MainThreadContext.runOnUiThread(() -> {
                            try {
                                MaterialFactory.makeOpaqueWithColor(context, new Color(rgb)).thenAccept(material1 -> promise.complete(material1));
                            } catch (Exception ex) {
                                promise.completeExceptionally(ex);
                            }
                        });
                        return promise;
                    });
        loadMaterial.thenAccept(this::setMaterial);
    }

    public void setMaterial(Material material) {
        if (this.material != material) {
            this.material = material;
            MainThreadContext.runOnUiThread(this::recreateRenderableOnUiThread);
        }
    }

    public void setShape(Shape shape) {
        if (this.shape != shape) {
            this.shape = shape;
            MainThreadContext.runOnUiThread(this::recreateRenderableOnUiThread);
        }
    }
    public Shape getShape() {
        return shape;
    }

    public void setMovable(boolean movable) {
        MainThreadContext.runOnUiThread(() -> {
            transformableNode.getTranslationController().setEnabled(movable);
            transformableNode.getRotationController().setEnabled(movable);
        });
    }

    public void destroy() {
        MainThreadContext.runOnUiThread(() -> {
            anchorNode.setRenderable(null);
            anchorNode.setParent(null);
            Anchor localAnchor =  anchorNode.getAnchor();
            if (localAnchor != null) {
                anchorNode.setAnchor(null);
                localAnchor.detach();
            }
        });
    }

    private void recreateRenderableOnUiThread() {
//        if (material != null) {
            Renderable renderable = null;
            CompletableFuture<ViewRenderable> build = null;
            switch (shape) {
                case Sphere:
                    renderable = ShapeFactory.makeSphere(
                            0.1f,
                            new Vector3(0.0f, 0.1f, 0.0f),
                            material);
                    break;
                case Cube:
                    renderable = ShapeFactory.makeCube(
                            new Vector3(0.161f, 0.161f, 0.161f),
                            new Vector3(0.0f, 0.0805f, 0.0f),
                            material);
                    break;
                case Cylinder:
                    renderable = ShapeFactory.makeCylinder(
                            0.0874f,
                            0.175f,
                            new Vector3(0.0f, 0.0875f, 0.0f),
                            material);
                    break;
                case CardPicker:
                    createCustomTestView();

                    break;
                case AnchorRegister:
                    createAnchorRegister();
                    break;
                default:
                    throw new IllegalStateException("Invalid shape");
            }
            if (shape != Shape.CardPicker) {
                transformableNode.setRenderable(renderable);
            }
//        }
    }

    private void createAnchorRegister() {
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.anchor_registration)
                .build()
                .thenAccept(
                        r1 -> {
                            transformableNode.setRenderable(r1);
                            ((TextView) r1.getView().findViewById(R.id.progress_guide_text)).setText(progressMessage);
                        });
    }

    private void createCustomTestView() {
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.picker_layout)
                .build()
                .thenAccept(
                        r -> {
                            transformableNode.setRenderable(r);
                            r.getView().findViewById(R.id.tv).setOnClickListener(view -> {
                                renderTvCard();
                            });
                            r.getView().findViewById(R.id.chair).setOnClickListener(view -> {
                                renderChairCard();
                            });
                            r.getView().findViewById(R.id.plant).setOnClickListener(view -> {
                                renderPlantCard();
                            });
                            r.getView().findViewById(R.id.sofa).setOnClickListener(view -> {
                                renderSofaCard();
                            });
                            r.getView().findViewById(R.id.misc).setOnClickListener(view -> {
                                renderMiscCard();
                            });
                            r.getView().setOnClickListener(v -> {
                                showMoreOptionsForCard();
                            });
                        });
    }

    private void renderTvCard() {
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.card_tv)
                .build()
                .thenAccept(
                        r1 -> {
                            transformableNode.setRenderable(r1);
                            r1.getView().findViewById(R.id.delete_card).setOnClickListener(view -> {
                                createCustomTestView();
                            });
                        });
    }
    private void renderChairCard() {
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.card_chair)
                .build()
                .thenAccept(
                        r1 -> {
                            transformableNode.setRenderable(r1);
                            r1.getView().findViewById(R.id.delete_card).setOnClickListener(view -> {
                                createCustomTestView();
                            });
                        });
    }

    private void renderPlantCard() {
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.card_plant)
                .build()
                .thenAccept(
                        r1 -> {
                            transformableNode.setRenderable(r1);
                            r1.getView().findViewById(R.id.delete_card).setOnClickListener(view -> {
                                createCustomTestView();
                            });
                        });
    }
    private void renderSofaCard() {
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.card_sofa)
                .build()
                .thenAccept(
                        r1 -> {
                            transformableNode.setRenderable(r1);
                            r1.getView().findViewById(R.id.delete_card).setOnClickListener(view -> {
                                createCustomTestView();
                            });
                        });
    }
    private void renderMiscCard() {
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.card_misc)
                .build()
                .thenAccept(
                        r1 -> {
                            transformableNode.setRenderable(r1);
                            r1.getView().findViewById(R.id.delete_card).setOnClickListener(view -> {
                                createCustomTestView();
                            });
                        });
    }

    private void showMoreOptionsForCard() {
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.card_more_options)
                .build()
                .thenAccept(
                        r1 -> {
                            transformableNode.setRenderable(r1);

                            r1.getView().findViewById(R.id.usecase).setOnClickListener(v -> {
//                                createCustomTestView();
//                                spatialAnchorsActivity.deleteCloudAnchorSync(cloudAnchor);
                            });
                        });
    }
}
