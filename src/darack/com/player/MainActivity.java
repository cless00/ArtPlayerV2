package darack.com.player;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class MainActivity extends Activity implements OnClickListener {
	
	private static final String TAG = "TEST_DEBUG";
	
	private ImageView logo;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        logo = (ImageView)findViewById(R.id.logo);
        
        logo.setOnClickListener(this);
    }
    
    
    @Override
	public void onClick(View view){
    	if(view == logo){
    		Log.d(TAG, "logo clicked. Starting Player...");
    		startActivity(new Intent(this, ArtPlayerActivity.class));
    		finish();
    	}
    }
}