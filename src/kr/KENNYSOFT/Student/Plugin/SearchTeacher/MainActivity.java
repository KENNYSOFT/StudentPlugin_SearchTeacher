package kr.KENNYSOFT.Student.Plugin.SearchTeacher;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity
{
	List<FavoriteItem> mList=new ArrayList<FavoriteItem>();
	MainSQLiteOpenHelper sql;
	RecyclerView mRecyclerView;
	WebView mWebView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setTitle(getIntent().getStringExtra("title"));
		
		CookieManager.getInstance().removeAllCookie();
		for(String cookie:getIntent().getStringExtra("cookie").split("; "))CookieManager.getInstance().setCookie("student.gs.hs.kr",cookie);
		
		sql=new MainSQLiteOpenHelper(this,"favorite.db",null,1);
		
		mWebView=(WebView)findViewById(R.id.webview);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.addJavascriptInterface(this,"studentplugin");
		try
		{
			mWebView.getSettings().setUserAgentString(mWebView.getSettings().getUserAgentString()+" StudentPlugin_SearchTeacher/"+this.getPackageManager().getPackageInfo(this.getPackageName(),PackageManager.GET_META_DATA).versionName);
		}
		catch(Exception e)
		{
		}
		mWebView.setWebChromeClient(new WebChromeClient());
		mWebView.setWebViewClient(new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view,String url)
			{
				if(VERSION.SDK_INT<VERSION_CODES.KITKAT)mWebView.loadUrl("javascript:select=function(id,name){window.studentplugin.addResult(id,name);}");
				else mWebView.evaluateJavascript("select=function(id,name){window.studentplugin.addResult(id,name);}",null);
			}
		});
		mWebView.loadUrl("http://student.gs.hs.kr"+getIntent().getStringExtra("url"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main,menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case R.id.action_favorite:
			mList.clear();
			Cursor c=sql.getReadableDatabase().query("favorite",null,null,null,null,null,"_id");
			while(c.moveToNext())
			{
				FavoriteItem it=new FavoriteItem();
				it.id=c.getString(c.getColumnIndex("id"));
				it.name=c.getString(c.getColumnIndex("name"));
				mList.add(it);
			}
			c.close();
			new AlertDialog.Builder(this).setIcon(R.drawable.ic_star_white_48dp).setTitle(R.string.action_favorite).setAdapter(new FavoriteAdapter(this,R.layout.favorite,mList),null).show();
			return true;
		case R.id.action_exit:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@JavascriptInterface
	public void addResult(final String id,final String name)
	{
		setResult(RESULT_OK,new Intent().putExtra("data","'"+id+"','"+name+"'"));
		if(sql.getReadableDatabase().query("favorite",null,"id='"+id+"'",null,null,null,"_id").getCount()==0)
		{
			new AlertDialog.Builder(this).setMessage(name+getString(R.string.message_add)).setPositiveButton(R.string.ok,null).setNeutralButton(R.string.message_favorite,new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog,int which)
				{
					ContentValues values=new ContentValues();
					values.put("id",id);
					values.put("name",name);
					sql.getWritableDatabase().insert("favorite",null,values);
				}
			}).setOnDismissListener(new DialogInterface.OnDismissListener()
			{
				public void onDismiss(DialogInterface dialog)
				{
					finish();
				}
			}).show();
		}
		else new AlertDialog.Builder(this).setMessage(name+getString(R.string.message_add)).setPositiveButton(R.string.ok,null).setOnDismissListener(new DialogInterface.OnDismissListener()
		{
			public void onDismiss(DialogInterface dialog)
			{
				finish();
			}
		}).show(); 
	}
}

class MainSQLiteOpenHelper extends SQLiteOpenHelper
{
	MainSQLiteOpenHelper(Context context,String name,CursorFactory factory,int version)
	{
		super(context,name,factory,version);
	}
	
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("create table favorite(_id INTEGER PRIMARY KEY AUTOINCREMENT,id TEXT,name TEXT);");
	}
	
	public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion)
	{
	}
}

class FavoriteItem
{
	String id,name;
}

class FavoriteAdapter extends ArrayAdapter<FavoriteItem>
{
	FavoriteAdapter(Context context,int resource,List<FavoriteItem> objects)
	{
		super(context,resource,objects);
	}

	@Override
	public View getView(final int position,View convertView,ViewGroup parent)
	{
		if(convertView==null)convertView=LayoutInflater.from(getContext()).inflate(R.layout.favorite,null);
		convertView.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				((MainActivity)getContext()).addResult(getItem(position).id,getItem(position).name);
			}
		});
		((TextView)convertView.findViewById(R.id.favorite_information)).setText(getItem(position).name);
		convertView.findViewById(R.id.favorite_delete).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				((MainActivity)getContext()).sql.getWritableDatabase().delete("favorite","id='"+getItem(position).id+"'",null);
				remove(getItem(position));
			}
		});
		return convertView;
	}
}