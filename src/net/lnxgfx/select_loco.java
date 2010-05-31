/*Copyright (C) 2010 Jason M'Sadoques
  jlyonm@gmail.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package net.lnxgfx;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.os.Message;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import android.widget.SimpleAdapter;
import android.widget.ListView;
import java.io.File;
import android.view.View;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.IOException;
import android.util.Log;
import java.io.FileReader;
import android.widget.EditText;
import android.content.Intent;
import android.widget.Button;
import android.widget.AdapterView;
import java.io.PrintWriter;

public class select_loco extends Activity
{
  ArrayList<HashMap<String, String> > engine_list;
  private SimpleAdapter list_adapter;

  ArrayList<Integer> engine_address_list;
  ArrayList<Integer> address_size_list; //Look at address_type.java

  int engine_address;
  int address_size;
  void acquire_engine()
  {
    Message acquire_msg=Message.obtain();
    acquire_msg.what=message_type.LOCO_ADDR;
    acquire_msg.arg1=engine_address;
    acquire_msg.arg2=address_size;
    threaded_application app=(threaded_application)this.getApplication();
    app.thread.comm_msg_handler.sendMessage(acquire_msg);

    //Save the engine list to the engine_list.txt file.
    File sdcard_path=Environment.getExternalStorageDirectory();
    File connections_list_file=new File(sdcard_path, "engine_driver/engine_list.txt");
    PrintWriter list_output;
    try
    {
      list_output=new PrintWriter(connections_list_file);
      //Add this connection to the head of connections list.
      list_output.format("%d:%d\n", engine_address, address_size);
      for(int i=0; i<engine_address_list.size(); i+=1)
      {
        if(engine_address==engine_address_list.get(i) && address_size==address_size_list.get(i)) { continue; }
        list_output.format("%d:%d\n", engine_address_list.get(i), address_size_list.get(i));
      }
      list_output.flush();
      list_output.close();
    }
    catch(IOException except)
    {
      Log.e("connection_activity", "Error creating a PrintWriter, IOException: "+except.getMessage());
    }
    //Start the throttle activity.
    Intent engine_driver=new Intent().setClass(this, engine_driver.class);
    startActivity(engine_driver);
  };

  public class button_listener implements View.OnClickListener
  {
    public void onClick(View v)
    {
      EditText entry=(EditText)findViewById(R.id.loco_address);
      engine_address=Integer.decode(entry.getText().toString());
      Spinner spinner=(Spinner)findViewById(R.id.address_length);
      address_size=spinner.getSelectedItemPosition();
      acquire_engine();
    };
  }

  public class engine_item implements AdapterView.OnItemClickListener
  {
    //When an item is clicked, acquire that engine.
    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
    {
      engine_address=engine_address_list.get(position);
      address_size=address_size_list.get(position);
      acquire_engine();
    };
  }

  //Handle pressing of the back button to disconnect from the WiThrottle server.
  @Override
  public boolean onKeyDown(int key, KeyEvent event)
  {
    if(key==KeyEvent.KEYCODE_BACK)
    {
      Message disconnect_msg=Message.obtain();
      disconnect_msg.what=message_type.DISCONNECT;
      threaded_application app=(threaded_application)this.getApplication();
      app.thread.comm_msg_handler.sendMessage(disconnect_msg);
    }
    return(super.onKeyDown(key, event));
  };

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.select_loco);

    //Set the options for the address length.
    Spinner address_spinner=(Spinner)findViewById(R.id.address_length);
    ArrayAdapter spinner_adapter=ArrayAdapter.createFromResource(this, R.array.address_size,
                                                                 android.R.layout.simple_spinner_item);
    spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    address_spinner.setAdapter(spinner_adapter);
    //Set up a list adapter to allow adding the list of recent engines to the UI.
    engine_list=new ArrayList<HashMap<String, String> >();
    list_adapter=new SimpleAdapter(this, engine_list, R.layout.engine_list_item, new String[] {"engine"},
                                   new int[] {R.id.engine_item_label});
    ListView engine_list_view=(ListView)findViewById(R.id.engine_list);
    engine_list_view.setAdapter(list_adapter);
    engine_list_view.setOnItemClickListener(new engine_item());

    engine_address_list=new ArrayList<Integer>();
    address_size_list=new ArrayList<Integer>();
    //Populate the ListView with the recent engines saved in a file. This will be stored in
    // /sdcard/engine_driver/engine_list.txt
    try
    {
      File sdcard_path=Environment.getExternalStorageDirectory();
      if(sdcard_path.canWrite())
      {
        File engine_driver_dir=new File(sdcard_path, "engine_driver");
        //The engine_driver directory should already exist. The connection activity should have already created it.
        if(engine_driver_dir.exists() && engine_driver_dir.isDirectory())
        {
          //TODO: Fix things if the path is not a directory.
          File engine_list_file=new File(engine_driver_dir, "engine_list.txt");
          if(engine_list_file.exists())
          {
            BufferedReader list_reader=new BufferedReader(new FileReader(engine_list_file));
            while(list_reader.ready())
            {
              String line=list_reader.readLine();
              engine_address_list.add(Integer.decode(line.substring(0, line.indexOf(':'))));
              address_size_list.add(Integer.decode(line.substring(line.indexOf(':')+1, line.length())));
              HashMap<String, String> hm=new HashMap<String, String>();
              hm.put("engine", engine_address_list.get(engine_address_list.size()-1).toString());
              engine_list.add(hm);
            }
            list_adapter.notifyDataSetChanged();
          }
        }
      }
    }
    catch (IOException except) { Log.e("connection_activity", "Could not read file "+except.getMessage()); }

    //Set the button callback.
    Button button=(Button)findViewById(R.id.acquire);
    button_listener click_listener=new button_listener();
    button.setOnClickListener(click_listener);
  };
}