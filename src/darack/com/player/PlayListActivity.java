package darack.com.player;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class PlayListActivity extends ListActivity implements OnClickListener {

	private static final String TAG = "TEST_DEBUG";
	private static final int DELETE = 11;

	private MusicInformation playList;
	
	private Button deleteAllBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playlist);

		setMyListAdapter();
		
		deleteAllBtn = (Button)findViewById(R.id.delete_all_btn);
		deleteAllBtn.setOnClickListener(this);
	}

	private void setMyListAdapter() {
		playList = new MusicInformation(this, R.layout.row,
				ArtPlayerActivity.playlist);

		setListAdapter(playList);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		registerForContextMenu(getListView());
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.d(TAG, "position : " + position);
		Log.d(TAG, "id : " + id);

		Intent i = new Intent();
		i.putExtra("POS", position);
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE, Menu.NONE, R.string.playlist_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		 AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		 
		switch(item.getItemId()){
		case DELETE:
			ArtPlayerActivity.playlist.remove(menuInfo.position);
			getListView().invalidateViews();
			return true;
		}
		return (super.onOptionsItemSelected(item));
	}
	

	private class MusicInformation extends ArrayAdapter<String> {
		private ArrayList<String> items;

		public MusicInformation(Context context, int textViewResourceId,
				ArrayList<String> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			View v = view;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.row, null);
			}
			String m = items.get(position);
			if (m != null) {
				ImageView imageview = (ImageView) v
						.findViewById(R.id.row_album_art);
				TextView tt = (TextView) v.findViewById(R.id.row_artist);
				TextView bt = (TextView) v.findViewById(R.id.row_title);

				Music musicInfo = Mp3TagReader.ReadMp3Info(m);
				
				if (tt != null) {
					tt.setText(musicInfo.getJemok() + " - " + musicInfo.getGasu());
				}
				if (bt != null) {
					bt.setText(m);
				}
				if(imageview != null && musicInfo.getAlbumArt() != null){
					imageview.setImageDrawable(musicInfo.getAlbumArt());
				} else{
					imageview.setImageResource(R.drawable.icon);
				}
			}

			return v;
		}
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		setResult(RESULT_CANCELED);
		finish();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case R.id.delete_all_btn:
			ArtPlayerActivity.playlist.clear();
			setMyListAdapter();
			break;
		}
	}
}