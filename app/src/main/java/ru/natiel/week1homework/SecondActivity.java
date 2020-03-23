package ru.natiel.week1homework;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        if(getIntent().getExtras()!= null){
            String title = getIntent().getExtras().getString("Title");
            Log.e("TAG", "Title is " + title);
        }
    }
}
