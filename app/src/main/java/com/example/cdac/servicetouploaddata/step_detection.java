package com.example.cdac.servicetouploaddata;

/**
 * Created by cdac on 23/6/15.
 */
public class step_detection {

    public float step_diff_count=0,step_normal_counter1=0,step_normal_counter2=0;
    public float step_time_delta=140;
    public float step_min_time_delta=20;
    public float step_amp_delta=(float) 1;
    public float step_amp_max_delta=(float)3.5;
    public float step_amp_neg_delta=(float)-3.5;
    public float step_found=0;
    public float step_ang=0;
    public float step_min=0;
    public static float step_min1=0,step_min2=0;
    public float step_max=0;
    public static float step_max1=0,step_max2=0;
    public float step_stage=1;
    public int step_detected=0;
    public float step_time_freq=0;


    public int main_step_detector(float step_z_acc_present,float step_z_acc_previous,float ang,float[] step_values_before_step_detected,float[] step_values_after_step_detected) {

        step_diff_count++;
        if (step_diff_count == step_time_delta && step_stage == 3) {
            step_values_before_step_detected[0] = step_min1;
            step_values_before_step_detected[1] = step_max1;
            step_values_before_step_detected[2] = step_min2;
            step_values_after_step_detected[0]=step_values_before_step_detected[0];
            step_values_after_step_detected[1]=step_values_before_step_detected[1];
            step_values_after_step_detected[2]=step_values_before_step_detected[2];
            step_values_after_step_detected[3]=step_values_before_step_detected[3];


            step_detected=1;
            step_found = 0;
            step_stage = 1;
            step_min1 = 0;
        }
        step_detector(step_z_acc_present,step_z_acc_previous,ang,step_values_before_step_detected,step_values_after_step_detected);

        return step_detected;
    }



    public void step_detector(float step_z_acc_present,float step_z_acc_previous,float ang,float[] step_values_before_step_detected,float[] step_values_after_step_detected) {

        //-----------------------------if rising occurs-------------------------------------
        if(step_z_acc_present > step_z_acc_previous)
        {
            step_normal_counter1++;
            step_max=step_z_acc_present;

            if(step_normal_counter2 > 0)//--------------min detected-----------
            {
                if(step_min <= 0)
                {
                    switch((int)step_stage)
                    {

                        case 1:
                            if(step_diff_count <= step_time_delta)
                            {
                                if(step_min<step_min1)         // again min1 detected-------------
                                {
                                    step_min1 = step_min;
                                    step_time_freq = 0;
                                    step_ang = ang;
                                    step_diff_count=0;
                                }

                            }
                            else if(step_diff_count > step_time_delta)     //time up so take it as minimum--------
                            {
                                step_min1 = step_min;
                                step_diff_count = 0;
                                step_time_freq = 0;
                                step_ang = ang;
                            }

                            break;


                        case 2:

                            if(step_diff_count <= step_time_delta)
                            {

                                if(((step_max1-step_amp_delta) >  step_min) &&(step_min>step_amp_neg_delta)&& step_diff_count > step_min_time_delta)
                                {
                                    step_stage=3;
                                    step_found=1;
                                    step_min2=step_min;
                                    step_diff_count=0;

                                }

                            }

                            else if(step_diff_count>step_time_delta)
                            {
                                if((step_max1-step_amp_delta) >  step_min && (step_min>step_amp_neg_delta))
                                {
                                    step_stage=3;
                                    step_found=1;
                                    step_min2=step_min;
                                    step_diff_count=0;
                                }

                            }

                            break;
                        case 3:
                            if(step_diff_count<=step_time_delta)
                            {
                                if(step_min<step_min2)
                                {
                                    step_min2=step_min;
                                    step_diff_count=0;
                                }

                            }
                            else if(step_diff_count>step_time_delta)
                            {
                                step_min1=step_min;
                                step_diff_count=0;
                                step_stage=1;
                                step_found=0;
                                step_time_freq=0;
                                step_ang=ang;
                            }

                            break;
                    }

                }

            }

            step_normal_counter2=0;
        }

        //---------------------------------if falling occurs----------------------------------------------
        else if(step_z_acc_present < step_z_acc_previous)
        {
            step_normal_counter2++;
            step_min = step_z_acc_present;

            if(step_normal_counter1 > 0)     //-------------max detected-----------
            {
                if(step_max > 0)
                {

                    switch((int)step_stage)
                    {
                        case 1:
                            if(step_diff_count <= step_time_delta)
                            {
                                if(((step_min1+step_amp_delta) <  step_max) &&(step_amp_max_delta>step_max)&& step_diff_count > step_min_time_delta)
                                {
                                    step_stage=2;
                                    step_max1=step_max;
                                    step_diff_count=0;
                                }

                            }
                            else if(step_diff_count>step_time_delta)
                            {
                                if((step_min1+step_amp_delta) <  step_max &&(step_amp_max_delta>step_max))
                                {
                                    step_stage=2;
                                    step_max1=step_max;
                                    step_diff_count=0;
                                }

                            }

                            break;
                        case 2:

                            if(step_diff_count <= step_time_delta)
                            {
                                if(step_max1 <  step_max)
                                {
                                    step_stage=2;
                                    step_max1=step_max;
                                    step_diff_count=0;
                                }

                            }
                            else if(step_diff_count > step_time_delta)
                            {
                                if(step_max>step_amp_delta)
                                {
                                    step_stage=2;
                                    step_max1=step_max;
                                    step_diff_count=0;
                                }


                            }

                            break;
                        case 3:
                            if(step_diff_count <= step_time_delta)
                            {
                                if((step_min2+step_amp_delta) <  step_max && (step_amp_max_delta>step_max) &&step_diff_count > step_min_time_delta)
                                {
                                    step_stage=2;

                                    step_values_before_step_detected[0]=step_min1;
                                    step_values_before_step_detected[1]=step_max1;
                                    step_values_before_step_detected[2]=step_min2;
                                    //                                 step_length();
                                    step_values_after_step_detected[0]=step_values_before_step_detected[0];
                                    step_values_after_step_detected[1]=step_values_before_step_detected[1];
                                    step_values_after_step_detected[2]=step_values_before_step_detected[2];
                                    step_values_after_step_detected[3]=step_values_before_step_detected[3];
                                    step_detected=1;
                                    step_min1=step_min2;
                                    step_max1=step_max;

                                    step_diff_count=0;
                                    step_found=0;
                                    step_ang=ang;
                                }

                            }
                            else if(step_diff_count > step_time_delta)
                            {
                                if((step_min2+step_amp_delta) <  step_max)
                                {
                                    step_stage=2;
                                    step_max1=step_max;
                                    step_diff_count=0;
                                    step_found=0;
                                }

                            }
                            break;
                    }
                }

            }
            step_normal_counter1=0;
        }

    }



}
