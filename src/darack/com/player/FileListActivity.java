package darack.com.player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FileListActivity extends ListActivity {

	private static final String TAG = "TEST_DEBUG";

	private MusicInformation fileList;
	private ArrayList<String> dirStack;

	private ImageButton play_list_btn;
	private ImageButton pre_dir_btn;
	private TextView directory_path;

	private static final String MEDIA_PATH = Environment
			.getExternalStorageDirectory().toString();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_list);

		directory_path = (TextView) findViewById(R.id.directory_path);
		play_list_btn = (ImageButton) findViewById(R.id.play_list_btn);
		play_list_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// 플레이리스트 오름차순 정렬
				Collections.sort(ArtPlayerActivity.playlist);
				
				setResult(RESULT_OK);
				finish();
			}
		});
		pre_dir_btn = (ImageButton) findViewById(R.id.pre_dir_btn);
		pre_dir_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (dirStack.size() < 2) {
					return;
				}
				// 마지막 폴더 지우고
				dirStack.remove(dirStack.size() - 1);
				// 상위 폴더로 이동
				String path = "";
				for (int i = 0; i < dirStack.size(); i++) {
					path += "/" + dirStack.get(i);
				}

				showDir(path);
			}
		});

		dirStack = new ArrayList<String>();
		dirStack.add(MEDIA_PATH);

		showDir(MEDIA_PATH);

		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		// 플레이리스트 오름차순 정렬
		Collections.sort(ArtPlayerActivity.playlist);
	}

	private void showDir(String path) {
		fileList = new MusicInformation(this, R.layout.row, getFileList(path));
		setListAdapter(fileList);

		directory_path.setText(path);
	}

	private ArrayList<MyFiles> getFileList(String path) {
		ArrayList<MyFiles> arrayList = new ArrayList<MyFiles>();

		File fp = new File(path);
		if (fp.exists() == false) {
			return null;
		}
		File[] files = fp.listFiles();

		// 디렉토리와 파일을 분리하기 위해 두개 반복문 사용
		for (int i = 0; i < files.length; i++) {
			MyFiles myFile = new MyFiles();

			if (!files[i].isHidden() && files[i].isDirectory()) {
				myFile.setPath(files[i].getPath());
				myFile.setFileName(files[i].getName());
				myFile.setDir(files[i].isDirectory());

				arrayList.add(myFile);

				Log.d(TAG, "path : " + myFile.getPath());
				Log.d(TAG, "filename : " + myFile.getFileName());
				Log.d(TAG, "dir : " + myFile.isDir());
			}
		}
		// 디렉토리 오름차순 정렬
		Collections.sort(arrayList);
		
		ArrayList<MyFiles> tempList = new ArrayList<MyFiles>();

		for (int i = 0; i < files.length; i++) {
			MyFiles myFile = new MyFiles();

			if (!files[i].isHidden() && files[i].isFile()) {
				String name = files[i].getName();
				StringTokenizer st = new StringTokenizer(name, ".");
				String extension = "";
				// 확장자 찾기
				while (st.hasMoreTokens()) {
					extension = st.nextToken();
				}
				if (extension.equalsIgnoreCase("mp3")) {
					myFile.setPath(files[i].getPath());
					myFile.setFileName(files[i].getName());
					myFile.setDir(files[i].isDirectory());

					tempList.add(myFile);

					Log.d(TAG, "path : " + myFile.getPath());
					Log.d(TAG, "filename : " + myFile.getFileName());
					Log.d(TAG, "dir : " + myFile.isDir());
				}
			}
		}
		// 파일 오름차순 정렬
		Collections.sort(tempList);
		
		// 리스트 통합
		for(int i = 0; i < tempList.size(); i++){
			arrayList.add(tempList.get(i));
		}

		return arrayList;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.d(TAG, "position : " + position);
		Log.d(TAG, "id : " + id);
		// v.setBackgroundColor(Color.CYAN);
		MyFiles myFile = (MyFiles) l.getAdapter().getItem(position);

		if (myFile.isDir()) {
			// 디렉토리면 내부 내용 표시
			// 어댑터 변경
			Log.d(TAG, "directory");
			showDir(myFile.getPath());
			dirStack.add(myFile.getFileName());
		} else {
			// 음악파일이면
			// 플레이리스트에 추가/제거
			// 플레이리스트에 이미 있으면
			if (ArtPlayerActivity.playlist.contains(myFile.getPath())) {
				// 제거
				Log.d(TAG, "remove file from playlist.");
				ArtPlayerActivity.playlist.remove(myFile.getPath());
			} else {
				// 추가
				Log.d(TAG, "add file to playlist.");
				ArtPlayerActivity.playlist.add(myFile.getPath());
			}
			Log.d(TAG, "mp3 file");

		}
	}

	private class MusicInformation extends ArrayAdapter<MyFiles> {
		private ArrayList<MyFiles> items;

		public MusicInformation(Context context, int textViewResourceId,
				ArrayList<MyFiles> items) {
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
			MyFiles m = items.get(position);
			if (m != null) {
				ImageView imageview = (ImageView) v
						.findViewById(R.id.row_album_art);
				TextView tt = (TextView) v.findViewById(R.id.row_artist);
				TextView bt = (TextView) v.findViewById(R.id.row_title);

				if (tt != null) {
					tt.setText(m.getFileName());
				}
				if (bt != null && m.isDir()) {
					bt.setText("Directory");
				} else {
//					Music musicInfo = Mp3TagReader.ReadMp3Info(m.getPath());
//					bt.setText(musicInfo.getGasu());
					bt.setText("");
				}
				if (imageview != null && m.isDir()) {
					imageview.setImageResource(R.drawable.dir_img);
				} else {
					imageview.setImageResource(R.drawable.icon);
				}

				if (ArtPlayerActivity.playlist.contains(m.getPath())) {
					v.setBackgroundColor(0xffffffff);
				} else {
					v.setBackgroundColor(Color.TRANSPARENT);
				}
			}

			return v;
		}
	}
}
