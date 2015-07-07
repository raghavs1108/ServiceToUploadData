package com.example.cdac.servicetouploaddata;

/**
 * Created by cdac on 23/6/15.
 */
public class averaging_filter {


    float[] window_buffer=new float[40];
    int counter=0;


    public float average(float value,float average,int window_size) {

        average -= window_buffer[counter];

        window_buffer[counter] = value/window_size;

        average += window_buffer[counter];

        //-------------------
        counter++;
        counter=(counter % window_size);

        return average;

    }
    public int return_data(){
        return counter;
    }


}