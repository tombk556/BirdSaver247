package htwd.s224.gruppe1.mnbirdsaver.util;

import android.graphics.Matrix;
import android.util.Log;

public class MatrixHelper {

    public String serializeMatrix(Matrix matrix) {
        float[] values = new float[9];
        matrix.getValues(values);
        StringBuilder serialized = new StringBuilder();
        for (float value : values) {
            serialized.append(value).append(","); // Werte durch Komma trennen
        }
        // Remove the last comma
        serialized.deleteCharAt(serialized.length() - 1);
        return serialized.toString();
    }


    public Matrix deserializeMatrix(String serializedMatrix) {
        String[] tokens = serializedMatrix.split(",");
        Log.d("Token: ", "Length: "+ tokens.length);
        System.out.println(tokens.length);
        float[] values = new float[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            values[i] = Float.parseFloat(tokens[i]);
        }
        Matrix matrix = new Matrix();
        matrix.setValues(values);
        return matrix;
    }

    public float[] pixelToGps(Matrix tranform_matrix, int pixelX, int pixelY) {
        float[] src = {pixelX, pixelY};
        float[] dst = new float[2];
        tranform_matrix.mapPoints(dst, src);

        return new float[]{dst[0], dst[1]};
    }

}
