package com.example.cdac.servicetouploaddata;

/**
 * Created by cdac on 23/6/15.
 */
public class step_length_estimation {


    public float step_length(float[] step_values_after_step_detected) {
        float temp=0;

        if (step_values_after_step_detected[0] > step_values_after_step_detected[2])
            temp = step_values_after_step_detected[2];

        else
            temp = step_values_after_step_detected[0];

        step_values_after_step_detected[3] = (float) ((float) (Math.sqrt(step_values_after_step_detected[1] - temp)) * 1.137);//1.129354352359816
        return step_values_after_step_detected[3];

    }


}

