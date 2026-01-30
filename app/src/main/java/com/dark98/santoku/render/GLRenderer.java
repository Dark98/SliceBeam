package com.dark98.santoku.render;

import static android.opengl.GLES30.*;
import static com.dark98.santoku.utils.DebugUtils.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.Log;

import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.List;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.dark98.santoku.R;
import com.dark98.santoku.Santoku;
import com.dark98.santoku.events.ObjectsListChangedEvent;
import com.dark98.santoku.events.SelectedObjectChangedEvent;
import com.dark98.santoku.slic3r.Bed3D;
import com.dark98.santoku.slic3r.GCodeProcessorResult;
import com.dark98.santoku.slic3r.GCodeViewer;
import com.dark98.santoku.slic3r.GLModel;
import com.dark98.santoku.slic3r.GLShaderProgram;
import com.dark98.santoku.slic3r.GLShadersManager;
import com.dark98.santoku.slic3r.Model;
import com.dark98.santoku.slic3r.Slic3rUtils;
import com.dark98.santoku.theme.ThemesRepo;
import com.dark98.santoku.utils.DoubleMatrix;
import com.dark98.santoku.utils.Prefs;
import com.dark98.santoku.utils.Vec3d;
import com.dark98.santoku.utils.ViewUtils;
import com.dark98.santoku.view.GLView;

public class GLRenderer implements GLSurfaceView.Renderer {
    private final static float FOV = 60f;
    private final static float NEAR_PLANE = 10f;
    private final static float FAR_PLANE = 1000f;

    private Camera camera = new Camera();
    private double[] projectionMatrix = new double[16];
    private double[] modelMatrix = new double[16];
    private double[] normalMatrix = new double[12];
    private double[] outModelMatrix = new double[16];

    private int viewportWidth, viewportHeight;

    private boolean cameraIsDirty = true;

    // Instance values, should be released
    private Bed3D bed;
    private boolean bedVisible = true;
    private int lastConfigUid;
    private GLShadersManager shadersManager;
    private GLModel backgroundModel;
    private GLModel selectionModel;
    private List<GLModel> glModels = new ArrayList<>();

    private Model model;

    private GCodeProcessorResult gcodeResult;
    private GCodeViewer viewer;
    private boolean isViewerEnabled;

    private int selectedObject = -1;
    private double selX, selY, selZ;
    private double selRotX, selRotY, selRotZ;
    private double selScaleX = 1, selScaleY = 1, selScaleZ = 1;

    private long lastDraw;
    private GLView glView;
    private Vec3d translate = new Vec3d();
    private Vec3d rotate = new Vec3d();
    private ArrayList<GLModel.HitResult> raycastHits = new ArrayList<>();

    private Vec3d bbMin = new Vec3d(), bbMax = new Vec3d();
    private boolean isInFlattenMode;
    private ArrayList<GLModel> flattenPlanes = new ArrayList<>();
    private static final double TOP_VIEW_MARGIN = 1.1;

    public Camera getCamera() {
        return camera;
    }

    public Bed3D getBed() {
        return bed;
    }

    public void setBedVisible(boolean visible) {
        bedVisible = visible;
    }

    public boolean isBedVisible() {
        return bedVisible;
    }

    public Bitmap renderToBitmap(int width, int height, boolean hideBed) {
        return renderToBitmap(width, height, hideBed, false);
    }

    public Bitmap renderToBitmap(int width, int height, boolean hideBed, boolean topView) {
        if (width <= 0 || height <= 0) {
            return null;
        }

        int[] fbo = new int[1];
        int[] texture = new int[1];
        int[] depth = new int[1];
        int[] fboMsaa = new int[1];
        int[] colorMsaa = new int[1];
        int[] depthMsaa = new int[1];

        glGenFramebuffers(1, fbo, 0);
        glGenTextures(1, texture, 0);
        glGenRenderbuffers(1, depth, 0);

        glBindTexture(GL_TEXTURE_2D, texture[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);

        glBindRenderbuffer(GL_RENDERBUFFER, depth[0]);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height);

        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture[0], 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depth[0]);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteRenderbuffers(1, depth, 0);
            glDeleteTextures(1, texture, 0);
            glDeleteFramebuffers(1, fbo, 0);
            return null;
        }

        boolean useMsaa = true;
        if (useMsaa) {
            glGenFramebuffers(1, fboMsaa, 0);
            glGenRenderbuffers(1, colorMsaa, 0);
            glGenRenderbuffers(1, depthMsaa, 0);

            glBindRenderbuffer(GL_RENDERBUFFER, colorMsaa[0]);
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, 4, GL_RGBA8, width, height);

            glBindRenderbuffer(GL_RENDERBUFFER, depthMsaa[0]);
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, 4, GL_DEPTH_COMPONENT16, width, height);

            glBindFramebuffer(GL_FRAMEBUFFER, fboMsaa[0]);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorMsaa[0]);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthMsaa[0]);

            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                useMsaa = false;
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glDeleteRenderbuffers(1, depthMsaa, 0);
                glDeleteRenderbuffers(1, colorMsaa, 0);
                glDeleteFramebuffers(1, fboMsaa, 0);
            }
        }

        int prevWidth = viewportWidth;
        int prevHeight = viewportHeight;
        boolean prevBed = bedVisible;
        CameraState prevCamera = null;

        viewportWidth = width;
        viewportHeight = height;
        if (useMsaa) {
            glBindFramebuffer(GL_FRAMEBUFFER, fboMsaa[0]);
        } else {
            glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        }
        glViewport(0, 0, width, height);
        bedVisible = !hideBed;
        if (topView) {
            prevCamera = applyTopViewCamera();
        }
        updateProjection();

        onDrawFrame(null);

        if (useMsaa) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, fboMsaa[0]);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo[0]);
            glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
            glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        }

        Bitmap bitmap = readPixelsToBitmap(width, height);

        if (prevCamera != null) {
            restoreCamera(prevCamera);
        }
        bedVisible = prevBed;
        viewportWidth = prevWidth;
        viewportHeight = prevHeight;
        glViewport(0, 0, prevWidth, prevHeight);
        updateProjection();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        if (useMsaa) {
            glDeleteRenderbuffers(1, depthMsaa, 0);
            glDeleteRenderbuffers(1, colorMsaa, 0);
            glDeleteFramebuffers(1, fboMsaa, 0);
        }
        glDeleteRenderbuffers(1, depth, 0);
        glDeleteTextures(1, texture, 0);
        glDeleteFramebuffers(1, fbo, 0);

        return bitmap;
    }

    private static Bitmap readPixelsToBitmap(int width, int height) {
        int[] buffer = new int[width * height];
        int[] source = new int[width * height];
        IntBuffer intBuffer = IntBuffer.wrap(buffer);
        intBuffer.position(0);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, intBuffer);
        int offset1, offset2;
        for (int i = 0; i < height; i++) {
            offset1 = i * width;
            offset2 = (height - i - 1) * width;
            for (int j = 0; j < width; j++) {
                int texturePixel = buffer[offset1 + j];
                int blue = (texturePixel >> 16) & 0xff;
                int red = (texturePixel << 16) & 0x00ff0000;
                source[offset2 + j] = (texturePixel & 0xff00ff00) | red | blue;
            }
        }
        return Bitmap.createBitmap(source, width, height, Bitmap.Config.ARGB_8888);
    }

    private CameraState applyTopViewCamera() {
        if (bed == null || !bed.isValid()) {
            return null;
        }
        Vec3d min;
        Vec3d max;
        if (model != null && model.getObjectsCount() > 0) {
            min = model.getBoundingBoxApproxMin();
            max = model.getBoundingBoxApproxMax();
        } else {
            min = bed.getVolumeMin();
            max = bed.getVolumeMax();
        }
        Vec3d center = min.center(max);
        double size = Math.max(max.x - min.x, max.y - min.y);
        if (size <= 0) {
            size = 1;
        }
        double fov = Math.toRadians(FOV);
        double distance = (size / 2.0) / Math.tan(fov / 2.0);
        distance *= TOP_VIEW_MARGIN;

        CameraState state = new CameraState(camera);
        camera.origin.set(center);
        camera.position.set(center.x, center.y, max.z + distance);
        camera.up.set(0, 1, 0);
        camera.setZoom(1f);
        return state;
    }

    private void restoreCamera(CameraState state) {
        camera.position.set(state.position);
        camera.origin.set(state.origin);
        camera.up.set(state.up);
        camera.setZoom(state.zoom);
    }

    private static final class CameraState {
        final Vec3d position;
        final Vec3d origin;
        final Vec3d up;
        final float zoom;

        CameraState(Camera camera) {
            position = new Vec3d(camera.position);
            origin = new Vec3d(camera.origin);
            up = new Vec3d(camera.up);
            zoom = camera.getZoom();
        }
    }

    public double[] getProjectionMatrix() {
        return projectionMatrix;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setGCodeViewer(GCodeProcessorResult result) {
        this.isViewerEnabled = result != null;
        this.gcodeResult = result;

        if (!isViewerEnabled && viewer != null) {
            viewer.release();
            viewer = null;
        }
    }

    public GLRenderer(GLView glView) {
        this.glView = glView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {}

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (bed != null) {
            onDestroy();
        }
        onCreate();
        glViewport(0, 0, viewportWidth = width, viewportHeight = height);
        updateProjection();
    }

    public void updateProjection() {
        if (bed == null || !bed.isValid()) return;
        float aspectRatio = (float) viewportWidth / viewportHeight;
        float invZoom = 1f / camera.getZoom();
        if (Prefs.isOrthoProjectionEnabled()) {
            Vec3d diff = bed.getVolumeMax().clone().add(bed.getVolumeMin().clone());
            double scale = (Math.max(diff.x, diff.y) / 2f + 10f) * invZoom;

            float ratioHorizontal = aspectRatio > 1 ? aspectRatio : 1;
            float ratioVertical = aspectRatio < 1 ? 1f / aspectRatio : 1;
            DoubleMatrix.orthoM(projectionMatrix, 0, -scale * ratioHorizontal, scale * ratioHorizontal, -scale * ratioVertical, scale * ratioVertical, NEAR_PLANE, FAR_PLANE);
        } else {
            DoubleMatrix.perspectiveM(projectionMatrix, 0, FOV * invZoom * (viewportWidth > viewportHeight ? 1 / aspectRatio : 1), aspectRatio, NEAR_PLANE, FAR_PLANE);
        }
    }

    public int getSelectedObject() {
        return selectedObject;
    }

    public void invalidateGlModel(int i) {
        if (model == null) return;
        if (i < glModels.size()) {
            GLModel glModel = glModels.get(i);
            glModel.reset();
            glModel.initFrom(model, i);
        }
    }

    public void invalidateSelectionObject() {
        if (selectionModel != null) {
            selectionModel.reset();
        }
    }

    public void resetGlModels() {
        if (model == null) return;
        for (int i = 0; i < model.getObjectsCount(); i++) {
            if (i >= glModels.size()) continue;

            GLModel glModel = glModels.get(i);
            glModel.reset();
            glModel.initFrom(model, i);
        }
    }

    public boolean invalidateFlattenMode() {
        if (isInFlattenMode) {
            setInFlattenMode(true);
            return true;
        }
        return false;
    }

    public boolean resetFlattenMode() {
        if (isInFlattenMode) {
            setInFlattenMode(false);
            return true;
        }
        return false;
    }

    public void setInFlattenMode(boolean inFlattenMode) {
        isInFlattenMode = inFlattenMode;

        for (int i = 0, c = flattenPlanes.size(); i < c; i++) {
            flattenPlanes.get(i).release();
        }
        flattenPlanes.clear();

        if (isInFlattenMode) {
            List<GLModel> planes = model.createFlattenPlanes(selectedObject);
            flattenPlanes.addAll(planes);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (backgroundModel == null) return; // Not initialized yet
        long dt = Math.min(System.currentTimeMillis() - lastDraw, 16);
        lastDraw = System.currentTimeMillis();

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glDisable(GL_DEPTH_TEST);
        GLShaderProgram shader = shadersManager.get(GLShadersManager.SHADER_BACKGROUND);
        shader.startUsing();
        shader.setUniformColor("top_color", ThemesRepo.getColor(R.attr.backgroundColorTop));
        shader.setUniformColor("bottom_color", ThemesRepo.getColor(R.attr.backgroundColorBottom));
        backgroundModel.render();
        shader.stopUsing();
        glEnable(GL_DEPTH_TEST);

        boolean bottom = Prefs.isOrthoProjectionEnabled() ? camera.getDirForward().z > 0 : camera.getDirToBed().z > 0;
        if (lastConfigUid != Santoku.CONFIG_UID) {
            configureBed();
        }
        if (bed.isValid() && bedVisible) {
            bed.render(shadersManager, bottom, camera.getViewModelMatrix(), projectionMatrix, 1f / camera.getZoom());
        }

        if (isViewerEnabled) {
            if (viewer == null) {
                viewer = new GCodeViewer();
                viewer.initGL();
                viewer.setThemeColors();
                viewer.load(gcodeResult);
            }

            viewer.render(camera.getViewModelMatrix(), projectionMatrix);
        }
        if (viewer == null && model != null) {
            shader = shadersManager.get(GLShadersManager.SHADER_GOURAUD_LIGHT);
            shader.startUsing();
            int color = ThemesRepo.getColor(android.R.attr.colorAccent);
            int hoverColor = ThemesRepo.getColor(R.attr.modelHoverColor);

            for (int i = 0; i < model.getObjectsCount(); i++) {
                boolean left = model.isLeftHanded(i);
                if (left) {
                    glFrontFace(GL_CW);
                }

                boolean selected = i == selectedObject;

                shader.setUniform("emission_factor", 0.05f);
                DoubleMatrix.setIdentityM(modelMatrix, 0);
                if (selected) {
                    DoubleMatrix.translateM(modelMatrix, 0, selX, selY, selZ);

                    model.getTranslation(i, translate);
                    model.getRotation(i, rotate);
                    DoubleMatrix.translateM(modelMatrix, 0, translate.x, translate.y, translate.z);
                    DoubleMatrix.rotateM(modelMatrix, 0, selRotX, 1, 0, 0);
                    DoubleMatrix.rotateM(modelMatrix, 0, selRotY, 0, 1, 0);
                    DoubleMatrix.rotateM(modelMatrix, 0, selRotZ, 0, 0, 1);
                    DoubleMatrix.scaleM(modelMatrix, 0, selScaleX, selScaleY, selScaleZ);
                    DoubleMatrix.translateM(modelMatrix, 0, -translate.x, -translate.y, -translate.z);
                }
                DoubleMatrix.multiplyMM(outModelMatrix, 0, camera.getViewModelMatrix(), 0, modelMatrix, 0);

                shader.setUniformMatrix4fv("view_model_matrix", outModelMatrix);
                shader.setUniformMatrix4fv("projection_matrix", projectionMatrix);

                Slic3rUtils.calcViewNormalMatrix(camera.getViewModelMatrix(), modelMatrix, normalMatrix);
                shader.setUniformMatrix3fv("view_normal_matrix", normalMatrix);

                shader.setUniform("volume_mirrored", left);

                if (glModels.size() < i + 1) {
                    GLModel glModel = new GLModel();
                    glModel.initFrom(model, i);
                    glModels.add(glModel);
                }
                GLModel glModel = glModels.get(i);
                boolean hovering = glModel.isHovering || selectedObject == i;
                // FIXME: Render is lagging out with hover progress
//                if (hovering && glModel.hoverProgress < 1) {
//                    glModel.hoverProgress = Math.min(glModel.hoverProgress + dt / 50f, 1);
//                    glView.queueEvent(() -> glView.requestRender());
//                } else if (!hovering && glModel.hoverProgress > 0) {
//                    glModel.hoverProgress = Math.max(glModel.hoverProgress - dt / 50f, 0);
//                    glView.queueEvent(() -> glView.requestRender());
//                }
                glModel.setColor(ColorUtils.blendARGB(color, hoverColor, hovering ? 1 : 0));
                glModel.render();

                if (left) {
                    glFrontFace(GL_CCW);
                }

                if (selected) {
                    shader.stopUsing();

                    GLShaderProgram flat = shadersManager.get(GLShadersManager.SHADER_FLAT);
                    glLineWidth(ViewUtils.dp(1.5f));

                    flat.startUsing();
                    flat.setUniformMatrix4fv("view_model_matrix", outModelMatrix);
                    flat.setUniformMatrix4fv("projection_matrix", projectionMatrix);

                    if (selectionModel == null) {
                        selectionModel = new GLModel();
                    }
                    selectionModel.initBoundingBox(model, i);
                    selectionModel.setColor(hoverColor);
                    selectionModel.render();

                    flat.stopUsing();

                    shader.startUsing();
                }

                if (isInFlattenMode) {
                    shader.stopUsing();

                    GLShaderProgram flat = shadersManager.get(GLShadersManager.SHADER_FLAT);

                    flat.startUsing();
                    glEnable(GL_BLEND);
                    flat.setUniformMatrix4fv("view_model_matrix", outModelMatrix);
                    flat.setUniformMatrix4fv("projection_matrix", projectionMatrix);

                    for (GLModel plane : flattenPlanes) {
                        boolean hoveringPlane = plane.isHovering;
                        int clr = ColorUtils.blendARGB(hoverColor, color, hoveringPlane ? 1 : 0);
                        plane.setColor(ColorUtils.setAlphaComponent(clr, (int) (Color.alpha(clr) * 0.75f)));
                        plane.render();
                    }

                    glDisable(GL_BLEND);
                    flat.stopUsing();

                    shader.startUsing();
                }
            }
            shader.stopUsing();
        }

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
    }

    public boolean deleteObject(int i) {
        if (model == null) return false;
        assertTrue(i >= 0 && i < model.getObjectsCount());

        model.deleteObject(i);
        if (glModels.size() > i) {
            glModels.remove(i).release();
        }
        if (i == selectedObject) {
            selectedObject = -1;
            selX = selY = selZ = 0;
            selRotX = selRotY = selRotZ = 0;
            selScaleX = selScaleY = selScaleZ = 1;
            Santoku.EVENT_BUS.fireEvent(new SelectedObjectChangedEvent());
        }

        if (model.getObjectsCount() == 0) {
            model.release();
            model = null;
        }
        Santoku.EVENT_BUS.fireEvent(new ObjectsListChangedEvent());
        return true;
    }

    public int raycastObjectIndex(float x, float y) {
        if (model == null) return -1;
        double minDistance = Double.MAX_VALUE;
        int j = -1;
        for (int i = 0, c = model.getObjectsCount(); i < c; i++) {
            if (i >= glModels.size()) continue;

            GLModel glModel = glModels.get(i);
            glModel.getRaycaster().raycast(this, raycastHits, x, y);

            boolean hovered = !raycastHits.isEmpty();
            if (hovered) {
                double distance = raycastHits.get(0).position.distance(camera.position);
                if (distance < minDistance) {
                    minDistance = distance;
                    j = i;
                }
            }
        }
        return j;
    }

    public boolean onClick(float x, float y) {
        if (model == null || isViewerEnabled) return false;

        int j = raycastObjectIndex(x, y);

        if (isInFlattenMode && (j == selectedObject || j == -1)) {
            int minPlane = -1;
            double minDistancePlane = Double.MAX_VALUE;

            for (int i = 0, c = flattenPlanes.size(); i < c; i++) {
                GLModel glModel = flattenPlanes.get(i);
                glModel.getRaycaster().raycast(this, raycastHits, x, y);

                double minDistanceRay = Double.MAX_VALUE;
                if (!raycastHits.isEmpty()) {
                    for (GLModel.HitResult res : raycastHits) {
                        double distance = res.position.distance(camera.position);
                        if (distance < minDistanceRay) {
                            minDistanceRay = distance;
                        }
                    }
                }
                if (minDistanceRay < minDistancePlane) {
                    minDistancePlane = minDistanceRay;
                    minPlane = i;
                }
            }

            if (minPlane != -1) {
                GLModel glModel = flattenPlanes.get(minPlane);
                model.flattenRotate(selectedObject, glModel);
                model.ensureOnBed(selectedObject);

                invalidateGlModel(selectedObject);
                for (int k = 0, l = flattenPlanes.size(); k < l; k++) {
                    flattenPlanes.get(k).release();
                }
                flattenPlanes.clear();

                selectedObject = -1;
                Santoku.EVENT_BUS.fireEvent(new SelectedObjectChangedEvent());
                return true;
            }

            return false;
        }

        boolean render = j != selectedObject || j != -1;
        selectedObject = j == selectedObject ? -1 : j;
        if (render) {
            if (isInFlattenMode) {
                setInFlattenMode(false);
            }
            if (selectedObject == -1) {
                selX = selY = selZ = 0;
                selRotX = selRotY = selRotZ = 0;
                selScaleX = selScaleY = selScaleZ = 1;
            }
            Santoku.EVENT_BUS.fireEvent(new SelectedObjectChangedEvent());
        }
        return render;
    }

    public boolean hover(float x, float y) {
        if (model == null || isViewerEnabled) return false;

        boolean render = false;
        double minDistance = Double.MAX_VALUE;
        GLModel minModel = null;
        for (int i = 0, c = model.getObjectsCount(); i < c; i++) {
            if (i >= glModels.size()) continue;

            GLModel glModel = glModels.get(i);
            glModel.getRaycaster().raycast(this, raycastHits, x, y);

            boolean hovered = !raycastHits.isEmpty();
            if (hovered) {
                double distance = raycastHits.get(0).position.distance(camera.position);
                if (distance < minDistance) {
                    minDistance = distance;
                    minModel = glModel;
                }
            }
        }
        for (int i = 0, c = model.getObjectsCount(); i < c; i++) {
            if (i >= glModels.size()) continue;

            GLModel glModel = glModels.get(i);

            boolean hovered = minModel == glModel;
            if (glModel.isHovering && !hovered) {
                glModel.isHovering = false;
                render = true;
            } else if (!glModel.isHovering && hovered) {
                glModel.isHovering = true;
                render = true;
            }
        }

        if (isInFlattenMode) {
            int minPlane = -1;
            double minDistancePlane = Double.MAX_VALUE;

            for (int i = 0, c = flattenPlanes.size(); i < c; i++) {
                GLModel glModel = flattenPlanes.get(i);
                glModel.getRaycaster().raycast(this, raycastHits, x, y);

                double minDistanceRay = Double.MAX_VALUE;
                if (!raycastHits.isEmpty()) {
                    for (GLModel.HitResult res : raycastHits) {
                        double distance = res.position.distance(camera.position);
                        if (distance < minDistanceRay) {
                            minDistanceRay = distance;
                        }
                    }
                }
                if (minDistanceRay < minDistancePlane) {
                    minDistancePlane = minDistanceRay;
                    minPlane = i;
                }
            }

            if (minPlane != -1) {
                for (int i = 0; i < flattenPlanes.size(); i++) {
                    flattenPlanes.get(i).isHovering = i == minPlane;
                }
                render = true;
            } else {
                for (int i = 0; i < flattenPlanes.size(); i++) {
                    if (flattenPlanes.get(i).isHovering) {
                        flattenPlanes.get(i).isHovering = false;
                        render = true;
                    }
                }
            }
        }

        return render;
    }

    public boolean stopHover() {
        if (model == null) return false;

        boolean render = false;
        for (int i = 0, c = model.getObjectsCount(); i < c; i++) {
            if (i >= glModels.size()) continue;

            GLModel glModel = glModels.get(i);
            if (glModel.isHovering) {
                glModel.isHovering = false;
                render = true;
            }
        }
        return render;
    }

    public void setSelectionRotation(double x, double y, double z) {
        selRotX = x;
        selRotY = y;
        selRotZ = z;
    }

    public void setSelectionScale(double x, double y, double z) {
        selScaleX = x;
        selScaleY = y;
        selScaleZ = z;
    }

    public void setSelectionTranslation(double x, double y, double z) {
        selX = x;
        selY = y;
        selZ = z;
    }

    public void setModel(Model model) {
        this.model = model;
        resetGlModels();
    }

    public Model getModel() {
        return model;
    }

    public GCodeProcessorResult getGcodeResult() {
        return gcodeResult;
    }

    public GCodeViewer getViewer() {
        return viewer;
    }

    private void configureBed() {
        try {
            lastConfigUid = Santoku.CONFIG_UID;
            Santoku.genCurrentConfig();
            bed.configure(Santoku.getCurrentConfigFile());
        } catch (Exception e) {
            Log.e("GLRenderer", "Failed to update config", e);
        }
    }

    private void onCreate() {
        bed = new Bed3D();
        configureBed();

        backgroundModel = new GLModel();
        backgroundModel.initBackgroundTriangles();
        shadersManager = new GLShadersManager();
        if (!bed.isValid()) return;

        if (cameraIsDirty) {
            Vec3d min = bed.getVolumeMin(), max = bed.getVolumeMax();
            Vec3d center = min.center(max);
            camera.origin.set(center);
            camera.origin.z = 0;

            camera.position.x = center.x - center.z * 2;
            camera.position.y = center.y - center.z * 2;
            camera.position.z = min.z + Math.sqrt(center.z * center.z * 8);
            cameraIsDirty = false;
        }
        if (isViewerEnabled) {
            viewer = new GCodeViewer();
            viewer.initGL();
            viewer.setThemeColors();
            viewer.load(gcodeResult);
        }
    }

    public void onDestroy() {
        if (shadersManager != null) {
            shadersManager.clearShaders();
            shadersManager = null;
        }
        if (backgroundModel != null) {
            backgroundModel.release();
            backgroundModel = null;
        }
        if (selectionModel != null) {
            selectionModel.release();
            selectionModel = null;
        }
        if (bed != null) {
            bed.release();
            bed = null;
        }
        if (viewer != null) {
            viewer.release();
            viewer = null;
        }
        for (int i = 0; i < glModels.size(); i++) {
            glModels.get(i).release();
        }
        glModels.clear();

        isInFlattenMode = false;
        for (int i = 0; i < flattenPlanes.size(); i++) {
            flattenPlanes.get(i).release();
        }
        flattenPlanes.clear();
    }
}
