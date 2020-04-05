package ru.natiel.week1homework;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    private EditText mNameEditText;
    private EditText mPriceEditText;


    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        mNameEditText = findViewById(R.id.name_edittext);
        mPriceEditText = findViewById(R.id.price_edittext);

        Button addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                String name = mNameEditText.getText().toString();
                String price = mPriceEditText.getText().toString();

                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(price)) {
                    setResult(
                            RESULT_OK,
                            new Intent().putExtra("name", name).putExtra("price", price));
                    finish();
                }
            }
        });
    }
}