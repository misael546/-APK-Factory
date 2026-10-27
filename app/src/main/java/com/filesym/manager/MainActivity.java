package com.filesym.manager;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int bg=Color.rgb(247,249,252), ink=Color.rgb(30,41,59), muted=Color.rgb(100,116,139), blue=Color.rgb(37,99,235), line=Color.rgb(226,232,240);
    private LinearLayout root, list;
    private TextView pathLabel, countLabel;
    private EditText search;
    private File currentDir;
    private final ArrayList<File> all=new ArrayList<>();
    private final ArrayList<File> history=new ArrayList<>();
    private boolean sortBySize=false, descending=false;
    private String query="";
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(Color.WHITE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);build();checkAccess();}
    private boolean hasAccess(){return Build.VERSION.SDK_INT<30 || android.os.Environment.isExternalStorageManager();}
    private void checkAccess(){
        if(hasAccess()){File d=Environment.getExternalStorageDirectory();openDir(d);return;}
        list.removeAllViews();pathLabel.setText("Permiso necesario");countLabel.setText("Android necesita autorizar el acceso a archivos");
        TextView msg=label("Para explorar carpetas como un administrador de archivos, Filesym necesita el permiso especial «Administrar todos los archivos». Android abrirá sus ajustes oficiales; puedes concederlo o volver sin activarlo.",15,muted,false);msg.setGravity(Gravity.CENTER);msg.setPadding(dp(22),dp(24),dp(22),dp(24));list.addView(msg);
        Button go=new Button(this);go.setText("Conceder acceso");go.setAllCaps(false);go.setTextColor(Color.WHITE);go.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue));list.addView(go,new LinearLayout.LayoutParams(-1,dp(52)));go.setOnClickListener(v->{try{Intent i=new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception e){startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));}});
        Button limited=new Button(this);limited.setText("Elegir una carpeta solamente");limited.setAllCaps(false);list.addView(limited,new LinearLayout.LayoutParams(-1,dp(48)));limited.setOnClickListener(v->pickLimitedFolder());
    }
    private void build(){
        root=col();root.setBackgroundColor(bg);
        LinearLayout head=col();head.setPadding(dp(18),dp(10),dp(18),dp(8));root.addView(head);
        LinearLayout brand=row();TextView mark=label("▰",29,blue,true);brand.addView(mark,new LinearLayout.LayoutParams(dp(42),dp(42)));LinearLayout names=col();names.addView(label("Filesym",24,ink,true));names.addView(label("Explorador de archivos",12,muted,false));brand.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView menu=label("⋮",26,ink,true);menu.setGravity(Gravity.CENTER);brand.addView(menu,new LinearLayout.LayoutParams(dp(42),dp(42)));menu.setOnClickListener(v->menu());head.addView(brand);
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(ink);search.setHintTextColor(muted);search.setHint("⌕   Buscar en esta carpeta");search.setTextSize(15);search.setPadding(dp(14),0,dp(14),0);search.setBackground(shape(Color.WHITE,12,line));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(48));sp.topMargin=dp(12);head.addView(search,sp);
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString().toLowerCase(Locale.ROOT);render();}public void afterTextChanged(android.text.Editable e){}});
        LinearLayout shortcuts=row();shortcuts.setPadding(dp(12),dp(8),dp(12),dp(8));String[] titles={"Interno","Descargas","Imágenes","Documentos"};String[] icons={"▣","↓","▧","▤"};for(int i=0;i<titles.length;i++){final int ix=i;LinearLayout tile=col();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(2),dp(8),dp(2),dp(8));tile.setBackground(shape(Color.WHITE,12,line));TextView ic=label(icons[i],21,blue,true);ic.setGravity(Gravity.CENTER);tile.addView(ic,new LinearLayout.LayoutParams(-1,dp(30)));TextView tx=label(titles[i],11,ink,true);tx.setGravity(Gravity.CENTER);tile.addView(tx);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(65),1);tp.setMargins(dp(3),0,dp(3),0);shortcuts.addView(tile,tp);tile.setOnClickListener(v->{if(!hasAccess()){checkAccess();return;}File base=Environment.getExternalStorageDirectory();File dest=ix==0?base:ix==1?new File(base,"Download"):ix==2?new File(base,"Pictures"):new File(base,"Documents");if(dest.isDirectory())openDir(dest);else toast("Esa carpeta no existe en este dispositivo.");});}root.addView(shortcuts);
        LinearLayout crumb=row();crumb.setPadding(dp(18),dp(7),dp(18),dp(7));pathLabel=label("Almacenamiento interno",13,muted,true);crumb.addView(pathLabel,new LinearLayout.LayoutParams(0,dp(34),1));TextView sort=label("⇅ Ordenar",12,blue,true);crumb.addView(sort);sort.setOnClickListener(v->sortMenu());root.addView(crumb);
        countLabel=label("Preparando archivos…",12,muted,false);countLabel.setPadding(dp(20),0,dp(20),dp(8));root.addView(countLabel);
        ScrollView scroll=new ScrollView(this);list=col();list.setPadding(dp(12),dp(3),dp(12),dp(18));scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bottom=row();bottom.setBackgroundColor(Color.WHITE);bottom.setPadding(dp(12),dp(5),dp(12),dp(5));String[] nav={"⌂\nInicio","▤\nArchivos","＋\nNueva carpeta"};for(int i=0;i<nav.length;i++){final int ix=i;TextView n=label(nav[i],12,ix==1?blue:muted,true);n.setGravity(Gravity.CENTER);bottom.addView(n,new LinearLayout.LayoutParams(0,dp(50),1));n.setOnClickListener(v->{if(ix==0){if(hasAccess())openDir(Environment.getExternalStorageDirectory());else checkAccess();}else if(ix==1)refresh();else createFolder();});}root.addView(bottom);
        setContentView(root);
    }
    private void pickLimitedFolder(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);startActivityForResult(i,9);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==9&&res==RESULT_OK&&data!=null){getPreferences(0).edit().putString("limited",data.getData().toString()).apply();toast("Carpeta seleccionada. Para navegación completa, activa el permiso especial.");}}
    @Override protected void onResume(){super.onResume();if(root!=null&&hasAccess()&&currentDir==null)openDir(Environment.getExternalStorageDirectory());}
    private void openDir(File dir){if(dir==null||!dir.isDirectory()){toast("No se puede abrir esa carpeta.");return;}currentDir=dir;all.clear();File[] files=dir.listFiles();if(files!=null)all.addAll(Arrays.asList(files));sortFiles();pathLabel.setText(shortPath(dir));render();}
    private String shortPath(File f){if(f==null)return "Archivos";String rootPath=Environment.getExternalStorageDirectory().getAbsolutePath();if(f.getAbsolutePath().equals(rootPath))return "Almacenamiento interno";String p=f.getAbsolutePath();return p.startsWith(rootPath)?p.substring(rootPath.length()).replaceFirst("^/",""):p;}
    private void sortFiles(){Collections.sort(all,(a,b)->{if(a.isDirectory()!=b.isDirectory())return a.isDirectory()?-1:1;int n=sortBySize?Long.compare(a.length(),b.length()):a.getName().compareToIgnoreCase(b.getName());if(descending)n=-n;return n;});}
    private void refresh(){if(hasAccess()&&currentDir!=null)openDir(currentDir);else checkAccess();}
    private void render(){if(list==null)return;list.removeAllViews();if(!hasAccess()){return;}ArrayList<File> shown=new ArrayList<>();for(File f:all)if(f.getName().toLowerCase(Locale.ROOT).contains(query))shown.add(f);countLabel.setText(shown.size()+" elementos");if(shown.isEmpty()){TextView empty=label(query.isEmpty()?"Esta carpeta está vacía":"No hay resultados",15,muted,false);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(44),0,dp(44));list.addView(empty);return;}
        if(currentDir!=null&&!currentDir.equals(Environment.getExternalStorageDirectory())){TextView up=label("↑   Carpeta anterior",14,blue,true);up.setPadding(dp(12),dp(12),dp(12),dp(12));up.setOnClickListener(v->{File p=currentDir.getParentFile();if(p!=null&&p.canRead())openDir(p);});list.addView(up);}
        for(File f:shown){LinearLayout item=row();item.setPadding(dp(10),dp(7),dp(8),dp(7));item.setBackground(shape(Color.WHITE,10,line));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(5);TextView icon=label(fileIcon(f),23,f.isDirectory()?Color.rgb(217,119,6):blue,true);icon.setGravity(Gravity.CENTER);item.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(44)));LinearLayout info=col();info.setPadding(dp(8),0,dp(3),0);TextView name=label(f.getName(),14,ink,true);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);info.addView(name);info.addView(label(f.isDirectory()?"Carpeta":formatSize(f.length())+" · "+extension(f),11,muted,false));item.addView(info,new LinearLayout.LayoutParams(0,-2,1));TextView more=label("⋮",22,muted,true);more.setGravity(Gravity.CENTER);item.addView(more,new LinearLayout.LayoutParams(dp(30),dp(42)));item.setOnClickListener(v->{if(f.isDirectory())openDir(f);else openFile(f);});item.setOnLongClickListener(v->{actions(f);return true;});more.setOnClickListener(v->actions(f));list.addView(item,lp);}
    }
    private String fileIcon(File f){if(f.isDirectory())return "📁";String e=extension(f);if("jpg jpeg png gif webp bmp".contains(e))return "▧";if("mp4 mkv mov webm".contains(e))return "▶";if("mp3 wav ogg m4a".contains(e))return "♫";if("pdf".equals(e))return "PDF";if("apk".equals(e))return "◆";if("zip rar 7z".contains(e))return "▣";return "▤";}
    private String extension(File f){String n=f.getName();int i=n.lastIndexOf('.');return i<0?"Archivo":n.substring(i+1).toLowerCase(Locale.ROOT);}
    private String formatSize(long n){if(n<1024)return n+" B";if(n<1048576)return new DecimalFormat("0.0").format(n/1024d)+" KB";if(n<1073741824)return new DecimalFormat("0.0").format(n/1048576d)+" MB";return new DecimalFormat("0.0").format(n/1073741824d)+" GB";}
    private void openFile(File f){try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".provider",f);Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(u,mime(f));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){toast("No hay una aplicación compatible para abrir este archivo.");}}
    private String mime(File f){String e=extension(f);if(e.equals("jpg")||e.equals("jpeg"))return "image/jpeg";if(e.equals("png"))return "image/png";if(e.equals("webp"))return "image/webp";if(e.equals("mp4"))return "video/mp4";if(e.equals("mp3"))return "audio/mpeg";if(e.equals("pdf"))return "application/pdf";if(e.equals("apk"))return "application/vnd.android.package-archive";if(e.equals("zip"))return "application/zip";return "*/*";}
    private void actions(File f){String[] a={"Abrir","Compartir","Detalles","Renombrar","Eliminar"};new AlertDialog.Builder(this).setTitle(f.getName()).setItems(a,(d,w)->{if(w==0){if(f.isDirectory())openDir(f);else openFile(f);}if(w==1)share(f);if(w==2)details(f);if(w==3)rename(f);if(w==4)delete(f);}).show();}
    private void share(File f){try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".provider",f);Intent i=new Intent(Intent.ACTION_SEND);i.setType(mime(f));i.putExtra(Intent.EXTRA_STREAM,u);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"Compartir archivo"));}catch(Exception e){toast("No se pudo compartir el archivo.");}}
    private void details(File f){new AlertDialog.Builder(this).setTitle("Detalles").setMessage("Nombre: "+f.getName()+"\nUbicación: "+f.getAbsolutePath()+"\nTipo: "+(f.isDirectory()?"Carpeta":mime(f))+"\nTamaño: "+formatSize(f.length())+"\nModificado: "+new Date(f.lastModified())).setPositiveButton("Cerrar",null).show();}
    private void rename(File f){EditText input=new EditText(this);input.setText(f.getName());input.setSelectAllOnFocus(true);new AlertDialog.Builder(this).setTitle("Renombrar").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{String n=input.getText().toString().trim();if(n.isEmpty())return;File dest=new File(f.getParentFile(),n);if(dest.exists()){toast("Ya existe un elemento con ese nombre.");return;}if(f.renameTo(dest))refresh();else toast("No se pudo renombrar.");}).show();}
    private void delete(File f){new AlertDialog.Builder(this).setTitle("Eliminar").setMessage("¿Eliminar "+f.getName()+"? Si es una carpeta, se eliminará su contenido.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{if(removeRecursively(f))refresh();else toast("No se pudieron eliminar algunos elementos.");}).show();}
    private boolean removeRecursively(File f){if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File x:c)if(!removeRecursively(x))return false;}return f.delete();}
    private void createFolder(){if(!hasAccess()){checkAccess();return;}EditText in=new EditText(this);in.setHint("Nombre de carpeta");new AlertDialog.Builder(this).setTitle("Nueva carpeta").setView(in).setNegativeButton("Cancelar",null).setPositiveButton("Crear",(d,w)->{String n=in.getText().toString().trim();if(n.isEmpty())return;if(new File(currentDir,n).mkdir())refresh();else toast("No se pudo crear la carpeta.");}).show();}
    private void sortMenu(){String[] opts={"Nombre (A–Z)","Tamaño","Invertir orden"};new AlertDialog.Builder(this).setTitle("Ordenar archivos").setItems(opts,(d,w)->{if(w==0){sortBySize=false;descending=false;}if(w==1)sortBySize=true;if(w==2)descending=!descending;sortFiles();render();}).show();}
    private void menu(){String[] opts={"Actualizar","Nueva carpeta","Ordenar","Ajustes de acceso","Acerca de Filesym"};new AlertDialog.Builder(this).setTitle("Filesym").setItems(opts,(d,w)->{if(w==0)refresh();if(w==1)createFolder();if(w==2)sortMenu();if(w==3){if(Build.VERSION.SDK_INT>=30)startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName())));else checkAccess();}if(w==4)new AlertDialog.Builder(this).setTitle("Filesym").setMessage("Explorador de archivos para Android. Versión 1.1").setPositiveButton("Cerrar",null).show();}).show();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){if(currentDir!=null&&!currentDir.equals(Environment.getExternalStorageDirectory())&&currentDir.getParentFile()!=null){openDir(currentDir.getParentFile());return;}super.onBackPressed();}
}