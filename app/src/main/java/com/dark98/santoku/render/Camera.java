package com.dark98.santoku.render;

import androidx.core.math.MathUtils;

import com.dark98.santoku.utils.DoubleMatrix;
import com.dark98.santoku.utils.Vec3d;

public class Camera {
    private double[] viewMatrix = new double[16];

    public Vec3d position = new Vec3d(0, 0, 0);
    public Vec3d origin = new Vec3d(0, 0, 0);
    public Vec3d up = new Vec3d(0, 0, 1);

    private double[] tempMatrix = new double[16];
    private float zoom = 1f;

    public Vec3d getDirToBed() {
        return origin.clone().multiply(1, 1, 0).add(position.clone().negate()).normalize();
    }

    public Vec3d getDirForward() {
        return origin.clone().add(position.clone().negate()).normalize();
    }

    public double[] getViewModelMatrix() {
        DoubleMatrix.setIdentityM(viewMatrix, 0);
        DoubleMatrix.setLookAtM(viewMatrix, 0, position.x, position.y, position.z, origin.x, origin.y, origin.z, up.x, up.y, up.z);
        return viewMatrix;
    }

    public float getZoom() {
        return zoom;
    }

    public void zoom(float zoom) {
        this.zoom = MathUtils.clamp(this.zoom + zoom / 25f, 1f, 10f);
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public Vec3d calcScreenMovement(float x, float y) {
        x /= zoom;
        y /= zoom;

        Vec3d dir = getDirForward();
        double yaw = Math.atan2(dir.x, dir.y);
        double pitch = Math.asin(-dir.z);
        Vec3d upMod = up.clone();
        upMod.x = Math.sin(pitch) * Math.sin(yaw);
        upMod.y = Math.sin(pitch) * Math.cos(yaw);
        upMod.z = Math.cos(pitch);

        Vec3d right = dir.crossProduct(upMod);

        Vec3d screenY = dir.crossProduct(right);
        Vec3d screenX = right.clone();

        screenX.multiply(x);
        screenY.multiply(y);

        return new Vec3d(screenX).add(screenY);
    }
    public void move(float x, float y) {
        Vec3d move = calcScreenMovement(x, y);
        position.add(move);
        origin.add(move);
    }

    public void rotateAround(double rx, double ry) {
        double[] v = position.clone().add(origin.clone().negate()).asDoubleArray();

        DoubleMatrix.setIdentityM(tempMatrix, 0);

        Vec3d dir = getDirForward();
        double yaw = Math.atan2(dir.x, dir.y);
        double pitch = Math.toDegrees(Math.asin(-dir.z));

        double mry = -ry;
        if (pitch + mry > 90) {
            mry = 0;
        } else if (pitch + mry < -90) {
            mry = 0;
        }

        DoubleMatrix.rotateM(tempMatrix, 0, -mry * Math.cos(yaw), 1, 0, 0);
        DoubleMatrix.rotateM(tempMatrix, 0, mry * Math.sin(yaw), 0, 1, 0);
        DoubleMatrix.rotateM(tempMatrix, 0, rx, 0, 0, 1);

        DoubleMatrix.multiplyMV(v, 0, tempMatrix, 0, v, 0);
        position.set(v[0] / v[3], v[1] / v[3], v[2] / v[3]);
        position.add(origin);
    }
}
