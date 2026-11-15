package com.tam.bookkeeping;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
  DB db; LinearLayout root, history; TextView summary; EditText income, cost; TextView dateText; Calendar chosen=Calendar.getInstance(); final int green=Color.rgb(19,105,75); DecimalFormat money=new DecimalFormat("#,##0.00");
  @Override public void onCreate(Bundle b){super.onCreate(b);db=new DB(this); render();}
  TextView label(String s,int sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.rgb(33,45,40));t.setPadding(0,10,0,8);return t;}
  LinearLayout section(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(20,15,20,15);l.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,14);root.addView(l,p);return l;}
  String day(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
  String thaiDate(Calendar c){return new SimpleDateFormat("dd/MM/yyyy",new Locale("th","TH")).format(c.getTime());}
  double number(EditText e){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception x){return 0;}}
  void render(){ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,24,20,30);root.setBackgroundColor(Color.rgb(242,247,243));scroll.addView(root);setContentView(scroll);
    TextView heading=label("ข้าวต้มปลาตาเอ็ม",25);heading.setTextColor(green);heading.setTypeface(null,1);root.addView(heading);root.addView(label("บัญชีร้าน • รายรับ / ค่าวัตถุดิบ / กำไรขั้นต้น",14));
    LinearLayout stats=section();stats.addView(label("สรุปยอดขาย",20));summary=label("",17);stats.addView(summary);
    LinearLayout form=section();form.addView(label("บันทึกยอดรายวัน",20));dateText=label("วันที่: "+thaiDate(chosen),17);form.addView(dateText);Button pick=new Button(this);pick.setText("เลือกวัน / เดือน / ปี");form.addView(pick);pick.setOnClickListener(v->{DatePickerDialog d=new DatePickerDialog(this,(view,y,m,dd)->{chosen.set(y,m,dd);dateText.setText("วันที่: "+thaiDate(chosen));loadDate();},chosen.get(Calendar.YEAR),chosen.get(Calendar.MONTH),chosen.get(Calendar.DAY_OF_MONTH));d.show();});
    income=new EditText(this);income.setHint("รายรับจากการขาย (บาท)");income.setInputType(8194);form.addView(income);cost=new EditText(this);cost.setHint("ค่าวัตถุดิบ (บาท)");cost.setInputType(8194);form.addView(cost);
    Button save=new Button(this);save.setText("บันทึก / อัปเดตยอดวันนี้");form.addView(save);save.setOnClickListener(v->{if(income.getText().toString().trim().isEmpty()||cost.getText().toString().trim().isEmpty()){Toast.makeText(this,"กรอกรายรับและค่าวัตถุดิบก่อน",Toast.LENGTH_SHORT).show();return;}double a=number(income),b=number(cost);if(a<0||b<0||!Double.isFinite(a)||!Double.isFinite(b)){Toast.makeText(this,"ยอดเงินต้องเป็นเลขตั้งแต่ 0 ขึ้นไป",Toast.LENGTH_SHORT).show();return;}db.save(day(chosen),a,b);Toast.makeText(this,"บันทึกแล้ว",Toast.LENGTH_SHORT).show();refresh();});
    Button del=new Button(this);del.setText("ลบข้อมูลของวันที่เลือก");form.addView(del);del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("ลบข้อมูลวันที่ "+thaiDate(chosen)+" ?").setNegativeButton("ยกเลิก",null).setPositiveButton("ลบ",(x,w)->{db.delete(day(chosen));loadDate();refresh();}).show());
    LinearLayout records=section();records.addView(label("รายการย้อนหลัง (ล่าสุด 30 วัน)",20));history=new LinearLayout(this);history.setOrientation(1);records.addView(history);loadDate();refresh();
  }
  void loadDate(){double[] v=db.get(day(chosen));income.setText(v==null?"":String.valueOf(v[0]));cost.setText(v==null?"":String.valueOf(v[1]));}
  void refresh(){Calendar now=Calendar.getInstance();String today=day(now);Calendar week=(Calendar)now.clone();week.setFirstDayOfWeek(Calendar.MONDAY);int offset=(week.get(Calendar.DAY_OF_WEEK)+5)%7;week.add(Calendar.DATE,-offset);Calendar month=(Calendar)now.clone();month.set(Calendar.DAY_OF_MONTH,1);
    summary.setText(line("วันนี้",db.sum(today,today))+"\n\n"+line("สัปดาห์นี้ (จันทร์–อาทิตย์)",db.sum(day(week),today))+"\n\n"+line("เดือนนี้",db.sum(day(month),today)));
    history.removeAllViews();Cursor c=db.getRecent();try{while(c.moveToNext()){String d=c.getString(0);double a=c.getDouble(1),b=c.getDouble(2);TextView item=label(d+"   ขาย "+money.format(a)+" ฿\nวัตถุดิบ "+money.format(b)+" ฿  |  เหลือ "+money.format(a-b)+" ฿",15);history.addView(item);View divider=new View(this);divider.setBackgroundColor(0xffdddddd);history.addView(divider,new LinearLayout.LayoutParams(-1,1));item.setOnClickListener(v->{String[] p=d.split("-");chosen.set(Integer.parseInt(p[0]),Integer.parseInt(p[1])-1,Integer.parseInt(p[2]));dateText.setText("วันที่: "+thaiDate(chosen));loadDate();});}}finally{c.close();}}
  String line(String name,double[] v){return name+"\nรายรับ "+money.format(v[0])+" ฿   |   วัตถุดิบ "+money.format(v[1])+" ฿\nเหลือ "+money.format(v[0]-v[1])+" ฿";}
  static class DB extends SQLiteOpenHelper {DB(Context c){super(c,"tam_shop.db",null,1);}public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE daily(day TEXT PRIMARY KEY,income REAL NOT NULL,cost REAL NOT NULL)");}public void onUpgrade(SQLiteDatabase d,int old,int newer){}
    void save(String day,double income,double cost){ContentValues v=new ContentValues();v.put("day",day);v.put("income",income);v.put("cost",cost);getWritableDatabase().insertWithOnConflict("daily",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    void delete(String day){getWritableDatabase().delete("daily","day=?",new String[]{day});}
    double[] get(String day){try(Cursor c=getReadableDatabase().rawQuery("SELECT income,cost FROM daily WHERE day=?",new String[]{day})){return c.moveToFirst()?new double[]{c.getDouble(0),c.getDouble(1)}:null;}}
    double[] sum(String start,String end){try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(income),0),COALESCE(SUM(cost),0) FROM daily WHERE day>=? AND day<=?",new String[]{start,end})){c.moveToFirst();return new double[]{c.getDouble(0),c.getDouble(1)};}}
    Cursor getRecent(){return getReadableDatabase().rawQuery("SELECT day,income,cost FROM daily ORDER BY day DESC LIMIT 30",null);}
  }
}
