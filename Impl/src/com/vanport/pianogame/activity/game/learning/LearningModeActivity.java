package com.vanport.pianogame.activity.game.learning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import jp.classmethod.sample.audiomixer.AudioMixer;
import jp.classmethod.sample.audiomixer.AudioMixer.WAVFile;
import jp.classmethod.sample.jpgtomp4.JPGToMP4;

import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.CropResolutionPolicy;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.BaseGameActivity;
import org.andengine.ui.activity.SimpleLayoutGameActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.vanport.pianogame.R;

public class LearningModeActivity extends SimpleLayoutGameActivity {
	private static final int CAMERA_WIDTH = 480;
    private static final int CAMERA_HEIGHT = 320;
    
    private static final int SHADOW_OFFSET = 200;
    private static final int NOTES_QUEUE_Y_START = -50;
    private static final int NOTES_QUEUE_Y_END = 200;
    
    private static final int NOTES_QUEUE = 4;
    
    float NOTES_CUR_TIME = 0;
    float NOTES_CUR_BEAT = 0;
    
    int BEAT_MAX;
    int OCTAVE_BASE;
    
    private BuildableBitmapTextureAtlas mBitmapTextureAtlas;

    private TextureRegion mOrbTextureRegion;
    private TextureRegion mShadowTextureRegion;
    
    
    List<NoteSprite> noteSprites = new ArrayList<NoteSprite>();
    List<NoteGroup> noteGroups = new ArrayList<NoteGroup>();
    List<NoteGroup> activeNoteGroups = new ArrayList<NoteGroup>();
    
    List<SoundAsset> noteSoundAssets = new ArrayList<SoundAsset>();
    
    
    int CUR_NOTEGROUPS = 0;
    int CUR_ACTIVESPRITEGROUP = 0;
    
    
    Scene curScene;
    
    
    List<WAVFile> wavRecordings = new ArrayList<WAVFile>();
    double timeElapsed = 0.0;
    
    
    
    boolean isGameover = false;
    
    
    AudioMixer audioMixer;
    
   
    @Override
    public EngineOptions onCreateEngineOptions() {
        // TODO Auto-generated method stub
        final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        EngineOptions options = new EngineOptions(true, ScreenOrientation.LANDSCAPE_SENSOR, new CropResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
        options.getAudioOptions().setNeedsMusic(true);
        options.getAudioOptions().setNeedsSound(true);
        return options;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.learning_mode_activity;
    }

    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.andengine_rendersurfaceview;
    }
    
    
    
    
    
    
    public class SoundAsset
    {
    	public String instrumentName;
    	public String scale;
    	public Sound sound;
    	public String path;
    	
    	public SoundAsset(String instrumentName, String scale, String path, BaseGameActivity activity)
    	{
    		this.instrumentName = instrumentName;
    		this.scale = scale;
    		try {
				this.sound = SoundFactory.createSoundFromAsset(activity.getSoundManager(), activity, path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		this.path = path;
    	}
    	
    	public void play()
    	{
    		if (sound != null)
    		{
    			sound.play();
    		}
    	}
    	
    	public void stop()
    	{
    		if (sound != null)
    		{
    			sound.stop();
    		}
    	}
    }
    
    
    
    
    
    
    
    
    
    @Override
    public void onCreateResources() {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        this.mBitmapTextureAtlas = new BuildableBitmapTextureAtlas(this.getTextureManager(), 1024, 1024, TextureOptions.BILINEAR_PREMULTIPLYALPHA);

        this.mOrbTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "orb_green.png");
        this.mShadowTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "shadow_white.png");
        
        try{ mBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 1, 1)); }
        catch(Exception e){ e.printStackTrace(); }

        this.mBitmapTextureAtlas.load();

    
    
    	SoundFactory.setAssetBasePath("sfx/");
    	
    	String jsonString = getJSONString("json/data_sfx_piano.json");
		JSONObject data_sfx_piano;
		try {
			data_sfx_piano = new JSONObject(jsonString);
			String prefixString = data_sfx_piano.optString("prefix");
			String instrumentName = data_sfx_piano.optString("instrumentName");
			JSONArray assets = data_sfx_piano.getJSONArray("assets");
			for (int i = 0; i < assets.length(); i++)
			{
				JSONObject asset = assets.getJSONObject(i);
				String suffixString = asset.optString("suffix");
				String extString = asset.optString("ext");
				
				String fileName = prefixString + suffixString + "." + extString;
				String scale = suffixString;
				
				SoundAsset soundAsset = new SoundAsset(instrumentName, scale, fileName, this);
				
				noteSoundAssets.add(soundAsset);
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
			data_sfx_piano = null;
		}
		
    }

    
    
    @Override
    public Scene onCreateScene() {
		// TODO Auto-generated method stub

		// this.mEngine.registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene();
		scene.getBackground().setColor(0.0f, 0.0f, 0.0f);

		Sprite shadow = new Sprite(40, SHADOW_OFFSET, this.mShadowTextureRegion, this.getVertexBufferObjectManager());
		scene.attachChild(shadow);

		scene.setTouchAreaBindingOnActionDownEnabled(true);
		
		try {
			makeNoteGroups();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		scene.registerUpdateHandler(new IUpdateHandler() {
			
			@Override
			public void reset() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onUpdate(float pSecondsElapsed) {
				// TODO Auto-generated method stub
				Log.e("ELAPSE", ""+timeElapsed);
            	timeElapsed += (double)pSecondsElapsed;
        		refreshNoteSpriteGroups();

			}
		});
		
		curScene = scene;
		
		
		audioMixer = new AudioMixer(this);
		
		return scene;
	}
    

    
    

    public class Note
    {
    	public int groupID;
    	public float time;
    	public float beat;
    	public int octave;
    	public String scale;
    	public float x;
    	
    	public Note(float time, float beat, String scale, int octave_base, int octave_offset, float x)
    	{
    		this.time = time;
    		this.beat = beat;
    		this.scale = scale;
    		this.octave = octave_base + octave_offset;
    		this.x = x;
    	}
    	
    	public String getActualScale()
    	{
    		String actualScaleString = scale + "" + octave;
    		return actualScaleString;
    	}
    }
    

    public class NoteGroup
    {
    	public int ID;
    	public float time;
    	public float beat;
    	public List<Note> notes = new ArrayList<Note>();
    	
    	public NoteGroup()
    	{

    	}
    	
    	public void addNote(Note note)
    	{
    		notes.add(note);
    	}
    }
    
    
    public class NoteSprite extends Sprite
    {
		public Note note;
		public boolean touchRegistered = false;
		public boolean isCreated = false;
		public boolean isActive = false;
		public boolean isTriggered = false;
		public boolean isDying = false;

		public NoteSprite(float pX, float pY, ITextureRegion pTextureRegion,
				VertexBufferObjectManager pVertexBufferObjectManager) 
    	{
			super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
		}
		
		public void linkNote(Note note)
		{
			this.note = note;
		}
    }
    
    
    
    
    
    void makeNoteGroups() throws JSONException 
    {
    	// fulfill notes groups array
    	
    	List<Note> _notes = new ArrayList<Note>();
    	
    	
    	String jsonString = getJSONString("json/data_game_notes_fur_elise_easy.json");
		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(jsonString);
		} catch (JSONException e) {
			e.printStackTrace();
			jsonObject = null;
		}
		
		int octave_base = jsonObject.getInt("octave_base");
		OCTAVE_BASE = octave_base;
		
		int beat_max = jsonObject.getInt("beat_max");
		BEAT_MAX = beat_max;
		
		JSONArray notes = jsonObject.getJSONArray("notes");
		
		for (int i = 0; i < notes.length(); i++)
		{
			JSONObject noteJson = notes.getJSONObject(i);
			
			float time = (float)noteJson.getDouble("time");
			float beat = (float)noteJson.getDouble("beat");
			float x = (float)noteJson.getDouble("x");
			String scale = noteJson.optString("scale");
			
			int octave_offset = noteJson.optInt("octave_offset", 0);
			
			Note note = new Note(time, beat, scale, octave_base, octave_offset, x);
			
			_notes.add(note);
		}
		
		
		
		
		
		noteGroups.clear();
		int k = 0;
		for (int i = 0; i < _notes.size(); )
		{
			NoteGroup noteGroup = new NoteGroup();
			
			Note noteA = _notes.get(i);
			
			noteGroup.time = noteA.time;
			noteGroup.beat = noteA.beat;
			
			noteA.groupID = k;
			
			noteGroup.addNote(noteA);
			
			for (int j = i + 1; j < _notes.size(); j++)
			{
				Note noteB = _notes.get(j);
				
				if (noteA.time == noteB.time
						&& noteA.beat == noteB.beat)
				{
					noteB.groupID = k;
					
					noteGroup.addNote(noteB);
				}
			}
			noteGroup.ID = k;
			noteGroups.add(noteGroup);
			
			k++;
			
			_notes.remove(i);
		}
		
		
		Collections.sort(noteGroups, new Comparator<NoteGroup>() 
			{

				@Override
				public int compare(NoteGroup lhs, NoteGroup rhs) {
					float diff = 0;
					if (lhs.time != rhs.time)
					{
						diff = lhs.time - rhs.time;
					}
					
					else if (lhs.beat != rhs.beat)
					{
						diff = lhs.beat - rhs.beat;
					}
					
					if (diff > 0)
					{
						return 1;
					}
					
					else if (diff < 0)
					{
						return -1;
					}
					
					else
					{
						return 0;
					}
				}
		
			}
		);
		
		
		
		
		
		
    }
    
    
    
    
    
	private String getJSONString(String filepath) {
		StringBuilder returnString = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(getApplicationContext().getAssets().open(filepath), "UTF-8"));
			String mLine = reader.readLine();
			while (mLine != null) {
				returnString.append(mLine);
				mLine = reader.readLine();
			}
		} catch (IOException e) {
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
		return returnString.toString();
	}

    
    
    
    
    
    
    void refreshNoteSpriteGroups()
    {
    	// if spritegroups array is less than notes queue,
    	// then append the next spritegroup
    	// first make the sprites
    	// then group them, link them with note object
    	// and then put into the queue
    	
    	// register touch if the note sprite is first created & is on the first of the queue
    	
    	// reposition them by:
    	// group 0 : NOTES QUEUE Y END
    	// group 1 : NOTES QUEUE Y END + (((NOTES QUEUE Y START - NOTES QUEUE Y END) * 2) / 6)
    	// group 2 : NOTES QUEUE Y END + (((NOTES QUEUE Y START - NOTES QUEUE Y END) * 4) / 6)
    	// group 3 : NOTES QUEUE Y START
    	refreshNoteSpriteGroupsSmuleMagicPianoMode();
    }
    
    
    void refreshNoteSpriteGroupsSmuleMagicPianoMode()
    {
    	/*
    	for (int i = 0; i < noteSprites.size(); i++)
    	{
    		NoteSprite noteSprite = noteSprites.get(i);
    		boolean isValid = false;
    		
    		
    		
    		for (int j = 0; j < activeNoteGroups.size(); j++)
    		{
    			NoteGroup noteGroup = activeNoteGroups.get(j);
    			if (noteSprite.note.groupID < CUR_ACTIVESPRITEGROUP)
    			{
    				
    			}
    			else if (noteSprite.note.groupID == noteGroup.ID)
    			{
    				isValid = true;
    				break;
    			}
    		}
    		
    		
    		if (isValid)
    		{
    			
    		}
    		
    		else 
    		{
//    			noteSprite.isTriggered = true;
//				noteSprite.isDying = true;
			}
    	}
    	*/
    	
    	
    	
    	for (int i = noteSprites.size() - 1; i >= 0; i--)
    	{
    		NoteSprite noteSprite = noteSprites.get(i);
    		if (noteSprite.isDying)
    		{
    			curScene.unregisterTouchArea(noteSprite);
    			noteSprite.detachSelf();
    			noteSprite.dispose();
    			noteSprites.remove(i);
    		}
    	}
    	
    	
    	
    	for (int i = activeNoteGroups.size() - 1; i >= 0; i--)
    	{
    		NoteGroup noteGroup = activeNoteGroups.get(i);
    		boolean isValid = false;
    		
    		for (int j = 0; j < noteSprites.size(); j++)
    		{
    			NoteSprite noteSprite = noteSprites.get(j);
    			if (noteGroup.ID == noteSprite.note.groupID)
    			{
    				if (!noteSprite.isTriggered
					 || !noteSprite.isDying)
    				{
    					isValid = true;
    					break;
    				}
    			}
    		}
    		
    		if (isValid)
    		{
    			
    		}
    		
    		else
    		{
    			activeNoteGroups.remove(i);
    			Log.e("SPRITE", "REMOVED!");
    		}
    	}
    	
    	
    	
    	while (activeNoteGroups.size() < NOTES_QUEUE
    			&& CUR_NOTEGROUPS < noteGroups.size())
    	{
    		NoteGroup noteGroup = noteGroups.get(CUR_NOTEGROUPS);
    		activeNoteGroups.add(noteGroup);
    		
    		for (int i = 0; i < noteGroup.notes.size(); i++)
    		{
    			Note note = noteGroup.notes.get(i);
    			NoteSprite noteSprite = new NoteSprite(
    					note.x, 
    					NOTES_QUEUE_Y_START, 
    					mOrbTextureRegion, 
    					getVertexBufferObjectManager())
    			{
    			    @Override
    			    public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float X, float Y) 
    			    {
    			        if (pSceneTouchEvent.isActionDown())
    			        {
    		    			Log.e("SPRITE", "TRIGGERED!");
    			            this.isTriggered = true;
    			        }
    			        return true;
    			    };
    			};
    			
    			noteSprite.note = note;
    			noteSprite.isCreated = true;
    			noteSprites.add(noteSprite);
    			
    			curScene.attachChild(noteSprite);
    			
    			Log.e("SPRITE", "CREATED!");
    		}
    		
    		CUR_NOTEGROUPS += 1;
    	}
    	
    	if (activeNoteGroups.size() > 0)
    	{
        	CUR_ACTIVESPRITEGROUP = activeNoteGroups.get(0).ID;
    	}


    	
    	
    	
    	for (int i = 0; i < noteSprites.size(); i++)
    	{
    		NoteSprite noteSprite = noteSprites.get(i);
    		boolean isValid = false;
    		boolean isLiving = false;
    		int curNoteSpriteGroupQueue = 0;
    		
    		for (int j = 0; j < activeNoteGroups.size(); j++)
    		{
    			NoteGroup noteGroup = activeNoteGroups.get(j);
    			if (noteSprite.note.groupID < CUR_ACTIVESPRITEGROUP)
    			{
    				
    			}
    			else if (noteSprite.note.groupID == noteGroup.ID)
    			{
    				isValid = true;
        			curNoteSpriteGroupQueue = j;
    				break;
    			}
    		}
    		
    		
    		if (isValid)
    		{
    			
    		}
    		
    		else 
    		{
    			noteSprite.isDying = true;
			}
    		
    		if (noteSprite.isCreated)
    		{
    			isLiving = true;
    		}
    		
    		
    		if (isLiving)
    		{
    			if (!noteSprite.isActive
    				&& noteSprite.note.groupID == CUR_ACTIVESPRITEGROUP)
    			{
    				noteSprite.isActive = true;
    				noteSprite.touchRegistered = true;
    				curScene.registerTouchArea(noteSprite);

        			Log.e("SPRITE", "ACTIVATED!");
    			}
    			
    			
        		if (noteSprite.isTriggered)
        		{
        			
        			// DONE : Make Music Note Here!
        			
        			for (int s = 0; s < noteSoundAssets.size(); s++)
        			{
        				SoundAsset soundAsset = noteSoundAssets.get(s);
        				if (soundAsset.instrumentName.equals("piano")
        					&& soundAsset.scale.toLowerCase().equals(noteSprite.note.getActualScale().toLowerCase()))
        				{
            				soundAsset.play();
        				}
        			}
        			
        			recordNote(noteSprite.note);
        			
        			noteSprite.isDying = true;
        			continue;
        		}
        		
        		
        		
        		float targetPosY = NOTES_QUEUE_Y_END;
                		
        		if (curNoteSpriteGroupQueue == 0)
        		{
        			targetPosY = NOTES_QUEUE_Y_END;
        		}
        		
        		else 
        		{
        			targetPosY = NOTES_QUEUE_Y_END + (((NOTES_QUEUE_Y_START - NOTES_QUEUE_Y_END) * curNoteSpriteGroupQueue) / NOTES_QUEUE - 1);
        		}
        		
        		
            	// reposition them by:
            	// group 0 : NOTES QUEUE Y END
            	// group 1 : NOTES QUEUE Y END + (((NOTES QUEUE Y START - NOTES QUEUE Y END) * 2) / 6)
            	// group 2 : NOTES QUEUE Y END + (((NOTES QUEUE Y START - NOTES QUEUE Y END) * 4) / 6)
            	// group 3 : NOTES QUEUE Y START
        		
        		float lerpedY = lerp(noteSprite.getY(), targetPosY, (float)0.40);
        		
        		noteSprite.setPosition(
        				noteSprite.getX(),
        				lerpedY);
    		}
    	}
    	
    	
    	
    	if (activeNoteGroups.size() <= 0
    		&& CUR_NOTEGROUPS >= noteGroups.size()
    		&& !isGameover)
    	{
    		isGameover = true;
    		
        	runOnUiThread(new Runnable() {
	    		
				@Override
				public void run() {
	        		Handler handler = new Handler();
		    		handler.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							outputRecordings();
						}
					}, 2000);
				}
        	});
    	}
    	
    }
    
    
    float lerp(float a, float b, float f)
    {
        return a + f * (b - a);
    }

    
    
    
    
    public void recordNote(Note note)
    {
    	WAVFile wavFile = audioMixer.createWAVFile();
    	wavFile.startTime = timeElapsed;

		for (int s = 0; s < noteSoundAssets.size(); s++)
		{
			SoundAsset soundAsset = noteSoundAssets.get(s);
			if (soundAsset.instrumentName.equals("piano")
				&& soundAsset.scale.toLowerCase().equals(note.getActualScale().toLowerCase()))
			{
				wavFile.assetPath = "sfx/" + soundAsset.path;
			}
		}
    			
		wavRecordings.add(wavFile);
    }
    
    
    
    
    public void outputRecordings()
    {
    	runOnUiThread(new Runnable() {
    	     @Override
    	     public void run() {

    	     	audioMixer.mixWAVFilesArray(wavRecordings, timeElapsed);
    	    	
    	     	outputVideo();
    	     	
    	         AlertDialog.Builder alert = new AlertDialog.Builder(LearningModeActivity.this);
    	         alert.setTitle("");
    	         alert.setMessage("Recordings outputted to ...  /sdcard/mixed.aac ");
    	         alert.setPositiveButton("OK", new OnClickListener() {
    	                 @Override
    	                 public void onClick(DialogInterface arg0, int arg1) {

    	                 }
    	         });

    	         alert.show();
    	     }
    	    });

    }
    
    
    
    

	public static Bitmap getBitmapFromAsset(Context context, String filePath) {
	    AssetManager assetManager = context.getAssets();

	    InputStream istr;
	    Bitmap bitmap = null;
	    try {
	        istr = assetManager.open(filePath);
	        bitmap = BitmapFactory.decodeStream(istr);
	    } catch (IOException e) {
	        // handle exception
	    }

	    return bitmap;
	}
	
	
    public void outputVideo()
    {
    	
		Bitmap bitmap1 = getBitmapFromAsset(this, "cover.png");
		Bitmap bitmap2 = getBitmapFromAsset(this, "cover2.png");
		File quickBitmap = new File(Environment.getExternalStorageDirectory() + "/cover.png");
		
		// JPG TO MP4
		JPGToMP4 videoJpgToMP4 = new JPGToMP4();
		File out = new File(Environment.getExternalStorageDirectory() + "/out.mp4");
//		try {
//			videoJpgToMP4.imageToMP4Quick(quickBitmap, out);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		try {
			videoJpgToMP4.imageToMP4(JPGToMP4.fromBitmap(bitmap1), JPGToMP4.fromBitmap(bitmap2), out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try {
			// 複数の動画を読み込み
			String f1 = Environment.getExternalStorageDirectory()
					+ "/out.mp4";
			Movie[] inMovies = new Movie[] { 
					MovieCreator.build(f1) };

			AACTrackImpl mp3Track = new AACTrackImpl(new FileDataSourceImpl(
					new File(Environment.getExternalStorageDirectory() + "/mixed.aac")));

			// 1つのファイルに結合
			List<Track> videoTracks = new LinkedList<Track>();
			List<Track> audioTracks = new LinkedList<Track>();
			for (Movie m : inMovies) {
				for (Track t : m.getTracks()) {
					if (t.getHandler().equals("soun")) {
						audioTracks.add(t);
					}
					if (t.getHandler().equals("vide")) {
						videoTracks.add(t);
					}
				}
			}
			Movie result = new Movie();
			if (audioTracks.size() > 0) {
				result.addTrack(new AppendTrack(audioTracks
						.toArray(new Track[audioTracks.size()])));
			}
			if (videoTracks.size() > 0) {
				result.addTrack(new AppendTrack(videoTracks
						.toArray(new Track[videoTracks.size()])));
			}
			//result.addTrack(mp3Track);
			result.addTrack(mp3Track);
			
			// 出力
			Container outputContainer = new DefaultMp4Builder().build(result);
			String outputFilePath = Environment
					.getExternalStorageDirectory() + "/output_append_sound.mp4";
			FileOutputStream fos = new FileOutputStream(new File(
					outputFilePath));
			outputContainer.writeContainer(fos.getChannel());
			fos.close();
			
			
			   Intent tostart = new Intent(Intent.ACTION_VIEW);

			   // String movieUrl = "android.resource://" + getPackageName() + "/raw/" + "myvideo.mp4";

			    tostart.setDataAndType(Uri.parse("file://" + outputFilePath), "video/*");
			    startActivity(tostart);  

			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    
}
