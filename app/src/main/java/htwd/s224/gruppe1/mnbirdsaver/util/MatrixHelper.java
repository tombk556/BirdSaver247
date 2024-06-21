/**
 * Utility class for handling operations related to Android's {@link Matrix}.
 */
package htwd.s224.gruppe1.mnbirdsaver.util;

import android.graphics.Matrix;
import android.util.Log;

public class MatrixHelper {

    /**
     * Serializes the given {@link Matrix} into a comma-separated string.
     *
     * @param matrix The matrix to be serialized.
     * @return A comma-separated string representing the matrix values.
     */
    public String serializeMatrix(Matrix matrix) {
        float[] values = new float[9];
        matrix.getValues(values);
        StringBuilder serialized = new StringBuilder();
        for (float value : values) {
            serialized.append(value).append(","); // Separate values with commas
        }
        // Remove the last comma
        serialized.deleteCharAt(serialized.length() - 1);
        return serialized.toString();
    }

    /**
     * Deserializes a comma-separated string into a {@link Matrix}.
     *
     * @param serializedMatrix The comma-separated string representing the matrix values.
     * @return The deserialized matrix.
     */
    public Matrix deserializeMatrix(String serializedMatrix) {
        String[] tokens = serializedMatrix.split(",");
        Log.d("Token: ", "Length: " + tokens.length);
        float[] values = new float[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            values[i] = Float.parseFloat(tokens[i]);
        }
        Matrix matrix = new Matrix();
        matrix.setValues(values);
        return matrix;
    }

    /**
     * Transforms pixel coordinates to GPS coordinates using the given transformation matrix.
     *
     * @param transformMatrix The matrix used to transform the coordinates.
     * @param pixelX The x-coordinate in pixels.
     * @param pixelY The y-coordinate in pixels.
     * @return An array containing the transformed GPS coordinates {latitude, longitude}.
     */
    public float[] pixelToGps(Matrix transformMatrix, int pixelX, int pixelY) {
        float[] src = {pixelX, pixelY};
        float[] dst = new float[2];
        transformMatrix.mapPoints(dst, src);

        return new float[]{dst[0], dst[1]};
    }
}
