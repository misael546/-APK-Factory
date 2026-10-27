package com.filesym.manager;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.DecimalFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_TREE = 41;
    private final int bg=Color.rgb(7,13,25), panel=Color.rgb(17,28,47), panel2=Color.rgb(23,39,62), blue=Color.rgb(34,153,255), muted=Color.rgb(151,174,205), white=Color.WHITE;
    private Uri treeUri, currentUri;
    private LinearLayout root, list, top;
    private TextView pathLabel, countLabel;
    private EditText searchBox;
    private final ArrayList<Doc> allDocs=new ArrayList<>();
    private final ArrayList<Uri> backStack=new ArrayList<>();
    private boolean gridMode=false;
    private String query="";
    private static class Doc {
        Uri uri; String name,mime; boolean dir; long size;
        Doc(Uri u,String n,String m,boolean d,long s){uri=u;name=n;mime=m;dir=d;size=s;}
    }

    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(0); treeUri=getPreferences(0).getString("tree","").isEmpty()?null:Uri.parse(getPreferences(0).getString("tree","")); currentUri=treeUri; build(); if(treeUri!=null) load(currentUri); else welcome();}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private void build(){
        root=col();root.setBackgroundColor(bg);
        top=col();top.setPadding(dp(18),dp(12),dp(18),dp(8));root.addView(top);
        LinearLayout bar=row();
        TextView logo=text("▰",28,blue,true);bar.addView(logo,new LinearLayout.LayoutParams(dp(40),dp(44)));
        LinearLayout brand=col();TextView title=text("Filesym",25,white,true);TextView sub=text("Tu mundo de archivos",12,muted,false);brand.addView(title);brand.addView(sub);bar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView viewBtn=text(gridMode?"☷":"▦",25,white,true);viewBtn.setGravity(Gravity.CENTER);viewBtn.setBackground(shape(panel,14,0));bar.addView(viewBtn,new LinearLayout.LayoutParams(dp(44),dp(44)));viewBtn.setOnClickListener(v->{gridMode=!gridMode;viewBtn.setText(gridMode?"☷":"▦");render();});
        top.addView(bar);
        searchBox=new EditText(this);searchBox.setSingleLine(true);searchBox.setTextColor(white);searchBox.setHintTextColor(muted);searchBox.setHint("⌕  Buscar archivos y carpetas");searchBox.setTextSize(14);searchBox.setPadding(dp(14),0,dp(14),0);searchBox.setBackground(shape(panel2,15,0));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(48));sp.topMargin=dp(14);top.addView(searchBox,sp);
        searchBox.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString().toLowerCase(Locale.ROOT);render();}public void afterTextChanged(android.text.Editable e){}});
        root.addView(makeStorageCard(),new LinearLayout.LayoutParams(-1,-2));
        LinearLayout crumb=row();crumb.setPadding(dp(18),dp(8),dp(18),dp(6));pathLabel=text("Almacenamiento",13,muted,false);crumb.addView(pathLabel,new LinearLayout.LayoutParams(0,dp(32),1));TextView refresh=text("↻",22,white,true);refresh.setGravity(Gravity.CENTER);crumb.addView(refresh,new LinearLayout.LayoutParams(dp(38),dp(34)));refresh.setOnClickListener(v->{if(currentUri!=null)load(currentUri);else welcome();});root.addView(crumb);
        countLabel=text("Selecciona una carpeta para empezar",12,muted,false);countLabel.setPadding(dp(20),0,dp(18),dp(8));root.addView(countLabel);
        ScrollView sc=new ScrollView(this);list=col();list.setPadding(dp(14),dp(2),dp(14),dp(20));sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=row();nav.setPadding(dp(12),dp(6),dp(12),dp(6));nav.setBackgroundColor(Color.rgb(10,20,35));
        String[] labels={"⌂\nInicio","▤\nArchivos","☆\nFavoritos","⚙\nAjustes"};for(int i=0;i<labels.length;i++){final int ix=i;TextView n=text(labels[i],12,i==1?blue:muted,true);n.setGravity(Gravity.CENTER);nav.addView(n,new LinearLayout.LayoutParams(0,dp(52),1));n.setOnClickListener(v->{if(ix==0)welcome();else if(ix==1){if(treeUri==null)pickFolder();else load(treeUri);}else if(ix==2)showMessage("Favoritos","Mantén pulsado un archivo para ver sus acciones.");else settings();});}root.addView(nav);
        setContentView(root);
    }
    private View makeStorageCard(){
        LinearLayout card=col();card.setPadding(dp(16),dp(14),dp(16),dp(14));card.setBackground(shape(panel,20,Color.rgb(28,54,82)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(16),dp(14),dp(16),dp(6));card.setLayoutParams(p);
        LinearLayout r=row();TextView icon=text("▣",28,blue,true);r.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(40)));LinearLayout info=col();info.addView(text("Almacenamiento del dispositivo",14,white,true));info.addView(text("Acceso seguro a tus carpetas",12,muted,false));r.addView(info,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=text("›",28,muted,false);r.addView(arrow);card.addView(r);card.setOnClickListener(v->pickFolder());return card;
    }
    private void welcome(){
        list.removeAllViews();pathLabel.setText("Inicio");countLabel.setText("Explora y organiza tus archivos");
        LinearLayout hero=col();hero.setPadding(dp(20),dp(25),dp(20),dp(25));hero.setGravity(Gravity.CENTER);hero.setBackground(shape(panel,24,0));
        TextView symbol=text("▰",58,blue,true);symbol.setGravity(Gravity.CENTER);hero.addView(symbol,new LinearLayout.LayoutParams(-1,dp(90)));
        TextView h=text("Todo en un solo lugar",22,white,true);h.setGravity(Gravity.CENTER);hero.addView(h);TextView desc=text("Imágenes, vídeos, documentos, descargas y más. Elige una carpeta para comenzar a explorar.",14,muted,false);desc.setGravity(Gravity.CENTER);desc.setPadding(0,dp(10),0,dp(20));hero.addView(desc);
        Button choose=new Button(this);choose.setText("Elegir carpeta");choose.setTextColor(white);choose.setAllCaps(false);choose.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue));hero.addView(choose,new LinearLayout.LayoutParams(-1,dp(50)));choose.setOnClickListener(v->pickFolder());list.addView(hero);
        String[][] features={{"▦","Explorador completo","Navega carpetas con búsqueda instantánea."},{"↗","Compartir archivos","Envía archivos mediante las apps instaladas."},{"✎","Administrar archivos","Crea carpetas, renombra o elimina elementos."},{"◈","Diseño moderno","Interfaz oscura con accesos rápidos."}};
        for(String[] f:features){LinearLayout r=row();r.setPadding(dp(12),dp(13),dp(12),dp(13));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);r.setBackground(shape(panel,16,0));TextView ic=text(f[0],24,blue,true);ic.setGravity(Gravity.CENTER);r.addView(ic,new LinearLayout.LayoutParams(dp(45),dp(45)));LinearLayout c=col();c.addView(text(f[1],14,white,true));c.addView(text(f[2],12,muted,false));r.addView(c);list.addView(r,lp);}
    }
    private void pickFolder(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_TREE);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==PICK_TREE&&res==RESULT_OK&&data!=null){treeUri=data.getData();try{getContentResolver().takePersistableUriPermission(treeUri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}getPreferences(0).edit().putString("tree",treeUri.toString()).apply();currentUri=treeUri;backStack.clear();load(currentUri);}}
    private String id(Uri u){return DocumentsContract.getDocumentId(u);}
    private void load(Uri folder){
        if(folder==null){welcome();return;}currentUri=folder;allDocs.clear();
        try{
            String docId=id(folder);Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,docId);
            Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE},null,null,null);
            if(c!=null){while(c.moveToNext()){String did=c.getString(0),name=c.getString(1),mime=c.getString(2);long size=c.isNull(3)?0:c.getLong(3);boolean dir=DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);Uri u=DocumentsContract.buildDocumentUriUsingTree(treeUri,did);allDocs.add(new Doc(u,name,mime,dir,size));}c.close();}
        }catch(Exception e){showMessage("No se pudo abrir la carpeta","Vuelve a elegir una carpeta y concede acceso a sus archivos.");}
        Collections.sort(allDocs,(a,b)->a.dir!=b.dir?(a.dir?-1:1):a.name.compareToIgnoreCase(b.name));
        pathLabel.setText(folder.equals(treeUri)?"Almacenamiento seleccionado":shortPath(id(folder)));render();
    }
    private String shortPath(String s){String[] a=s.split("/");return a.length==0?s:a[a.length-1];}
    private void render(){
        if(list==null)return;list.removeAllViews();if(treeUri==null){welcome();return;}
        ArrayList<Doc> visible=new ArrayList<>();for(Doc d:allDocs)if(d.name.toLowerCase(Locale.ROOT).contains(query))visible.add(d);
        countLabel.setText(visible.size()+" elementos"+(query.isEmpty()?"":" · búsqueda"));
        if(visible.isEmpty()){TextView empty=text(query.isEmpty()?"Esta carpeta está vacía":"No se encontraron resultados",15,muted,false);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(45),0,dp(45));list.addView(empty);return;}
        for(Doc d:visible){LinearLayout item=row();item.setPadding(dp(12),dp(9),dp(10),dp(9));item.setBackground(shape(panel,15,0));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(7);
            TextView ic=text(fileIcon(d),27,d.dir?Color.rgb(255,190,58):iconColor(d.mime),true);ic.setGravity(Gravity.CENTER);item.addView(ic,new LinearLayout.LayoutParams(dp(46),dp(48)));
            LinearLayout info=col();info.setPadding(dp(8),0,dp(4),0);TextView nm=text(d.name,14,white,true);nm.setMaxLines(1);nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);info.addView(nm);info.addView(text(d.dir?"Carpeta":formatSize(d.size)+" · "+typeName(d.mime),11,muted,false));item.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            TextView more=text("⋮",22,muted,true);more.setGravity(Gravity.CENTER);item.addView(more,new LinearLayout.LayoutParams(dp(30),dp(42)));
            item.setOnClickListener(v->{if(d.dir){backStack.add(currentUri);load(d.uri);}else openFile(d);});item.setOnLongClickListener(v->{actions(d);return true;});more.setOnClickListener(v->actions(d));list.addView(item,lp);
        }
        if(currentUri!=null&&!currentUri.equals(treeUri)){TextView back=text("‹  Carpeta anterior",14,blue,true);back.setPadding(dp(12),dp(15),dp(12),dp(15));back.setOnClickListener(v->{if(!backStack.isEmpty())load(backStack.remove(backStack.size()-1));else load(treeUri);});list.addView(back,0);}
    }
    private String fileIcon(Doc d){if(d.dir)return "📁";String m=d.mime==null?"":d.mime;if(m.startsWith("image/"))return "▧";if(m.startsWith("video/"))return "▶";if(m.startsWith("audio/"))return "♫";if(m.contains("pdf"))return "▤";if(m.contains("zip")||m.contains("rar")||m.contains("compressed"))return "▣";if(m.contains("apk")||d.name.toLowerCase().endsWith(".apk"))return "◆";return "▤";}
    private int iconColor(String m){if(m==null)return blue;if(m.startsWith("image/"))return Color.rgb(61,220,170);if(m.startsWith("video/"))return Color.rgb(188,100,255);if(m.startsWith("audio/"))return Color.rgb(255,105,160);return blue;}
    private String typeName(String m){if(m==null)return "Archivo";if(m.startsWith("image/"))return "Imagen";if(m.startsWith("video/"))return "Vídeo";if(m.startsWith("audio/"))return "Audio";if(m.contains("pdf"))return "PDF";return "Archivo";}
    private String formatSize(long n){if(n<1024)return n+" B";if(n<1048576)return new DecimalFormat("0.0").format(n/1024d)+" KB";if(n<1073741824)return new DecimalFormat("0.0").format(n/1048576d)+" MB";return new DecimalFormat("0.0").format(n/1073741824d)+" GB";}
    private void openFile(Doc d){try{Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(d.uri,d.mime==null?"*/*":d.mime);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){showMessage("No hay una app compatible","No se encontró una aplicación para abrir este archivo.");}}
    private void actions(Doc d){String[] options={"Abrir","Compartir","Renombrar","Eliminar"};new AlertDialog.Builder(this).setTitle(d.name).setItems(options,(dialog,which)->{if(which==0){if(d.dir){backStack.add(currentUri);load(d.uri);}else openFile(d);}if(which==1)share(d);if(which==2)rename(d);if(which==3)delete(d);}).show();}
    private void share(Doc d){Intent i=new Intent(Intent.ACTION_SEND);i.setType(d.mime==null?"*/*":d.mime);i.putExtra(Intent.EXTRA_STREAM,d.uri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(Intent.createChooser(i,"Compartir con"));}catch(Exception e){showMessage("No se pudo compartir","Intenta de nuevo.");}}
    private void rename(Doc d){EditText input=new EditText(this);input.setText(d.name);input.setSelectAllOnFocus(true);new AlertDialog.Builder(this).setTitle("Renombrar archivo").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(x,w)->{String name=input.getText().toString().trim();if(name.isEmpty()){showMessage("Nombre no válido","Escribe un nombre.");return;}try{DocumentsContract.renameDocument(getContentResolver(),d.uri,name);load(currentUri);}catch(Exception e){showMessage("No se pudo renombrar","Esta carpeta no permite cambiar el nombre.");}}).show();}
    private void delete(Doc d){new AlertDialog.Builder(this).setTitle("Eliminar elemento").setMessage("¿Quieres eliminar «"+d.name+"»? Esta acción no se puede deshacer.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(x,w)->{try{DocumentsContract.deleteDocument(getContentResolver(),d.uri);load(currentUri);}catch(Exception e){showMessage("No se pudo eliminar","Android o esta carpeta no permite eliminar este elemento.");}}).show();}
    private void createFolder(){if(currentUri==null){pickFolder();return;}EditText in=new EditText(this);in.setHint("Nombre de la carpeta");new AlertDialog.Builder(this).setTitle("Nueva carpeta").setView(in).setNegativeButton("Cancelar",null).setPositiveButton("Crear",(d,w)->{String n=in.getText().toString().trim();if(n.isEmpty())return;try{DocumentsContract.createDocument(getContentResolver(),currentUri,DocumentsContract.Document.MIME_TYPE_DIR,n);load(currentUri);}catch(Exception e){showMessage("No se pudo crear","Elige una carpeta con permiso de escritura.");}}).show();}
    private void settings(){String[] opts={"Elegir otra carpeta","Crear carpeta","Cambiar vista","Acerca de Filesym"};new AlertDialog.Builder(this).setTitle("Ajustes de Filesym").setItems(opts,(d,w)->{if(w==0)pickFolder();if(w==1)createFolder();if(w==2){gridMode=!gridMode;render();}if(w==3)showMessage("Filesym 1.0","Explorador de archivos Android. Los permisos de carpetas los controla Android.");}).show();}
    @Override public boolean onCreateOptionsMenu(android.view.Menu menu){menu.add("Nueva carpeta").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);menu.add("Elegir carpeta");return true;}
    @Override public boolean onOptionsItemSelected(MenuItem item){if(item.getTitle().toString().equals("Nueva carpeta"))createFolder();else pickFolder();return true;}
    private void showMessage(String title,String msg){new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton("Entendido",null).show();}
    @Override public void onBackPressed(){if(currentUri!=null&&!currentUri.equals(treeUri)){if(!backStack.isEmpty())load(backStack.remove(backStack.size()-1));else load(treeUri);}else if(!query.isEmpty()){searchBox.setText("");}else super.onBackPressed();}
}
