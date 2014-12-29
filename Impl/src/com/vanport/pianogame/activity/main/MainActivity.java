package com.vanport.pianogame.activity.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.vanport.pianogame.R;
import com.vanport.pianogame.activity.game.learning.LearningModeActivity;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				startGame();
			}
		}, 1000);
	}

	public void startGame() {
		Intent newActivity = new Intent(MainActivity.this, LearningModeActivity.class);
		startActivity(newActivity);

	}
}
