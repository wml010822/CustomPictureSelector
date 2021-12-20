package com.luck.pictureselector;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SimpleActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other);
        Button btn_activity = findViewById(R.id.btn_activity);
        Button btn_inject_fragment = findViewById(R.id.btn_inject_fragment);
        btn_activity.setOnClickListener(this);
        btn_inject_fragment.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_activity) {
            startActivity(new Intent(SimpleActivity.this, MainActivity.class));
        } else if (v.getId() == R.id.btn_inject_fragment){
            startActivity(new Intent(SimpleActivity.this, InjectFragmentActivity.class));
        }
    }
}
