package com.calvindo.aldi.sutanto.tubes;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.calvindo.aldi.sutanto.tubes.API.ApiClient;
import com.calvindo.aldi.sutanto.tubes.API.ApiInterface;
import com.calvindo.aldi.sutanto.tubes.API.TransaksiResponse;
import com.calvindo.aldi.sutanto.tubes.Database.TransaksiClientAccess;
import com.calvindo.aldi.sutanto.tubes.adapter.TransaksiAdminAdapter;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListTransaksiActivity extends AppCompatActivity {
    final private int REQUEST_CODE_ASK_PERMISSIONS = 101;
    private ImageButton back;
    private RecyclerView rv;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TransaksiAdminAdapter transaksiAdapter;
    private List<TransaksiClientAccess> lsitTransaksi = new ArrayList<>();
    private ProgressBar progressBar;

    //untuk bikin pdf
    private PdfViewModel pdfViewModel;
    private File pdfFile;
    private PdfWriter writer;
    private AlertDialog.Builder builder;
    private TextView cetak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_list_transaksi);

        back = findViewById(R.id.back);
        rv = findViewById(R.id.rv_transaksi);
        progressBar = findViewById(R.id.progressBar);
        pdfViewModel = new ViewModelProvider(this).get(PdfViewModel.class);

        getTransaksi();

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ListTransaksiActivity.this, DashboardActivity.class);
                startActivity(i);
            }
        });

        swipeRefreshLayout = findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getTransaksi();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        cetak = findViewById(R.id.tvCetak);
        cetak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder = new AlertDialog.Builder(ListTransaksiActivity.this);

                builder.setCancelable(false);
                builder.setMessage("Cetak laporan transaksi?");
                builder.setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try{
                            createPdfWrapper();
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }catch (DocumentException e){
                            e.printStackTrace();
                        }
                    }
                });
                builder.setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
                alert.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                alert.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                alert.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE);
                alert.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);


            }
        });





    }

    public void getTransaksi(){
        progressBar.setVisibility(View.VISIBLE);
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<TransaksiResponse> call = apiService.getTransaksi();

        call.enqueue(new Callback<TransaksiResponse>() {
            @Override
            public void onResponse(Call<TransaksiResponse> call, Response<TransaksiResponse> response) {
                lsitTransaksi = response.body().getTransaksi();
                generateList(response.body().getTransaksi());
                progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onFailure(Call<TransaksiResponse> call, Throwable t) {

            }
        });
    }

    public void generateList(List<TransaksiClientAccess> transaksiList){
        transaksiAdapter = new TransaksiAdminAdapter(ListTransaksiActivity.this, transaksiList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(ListTransaksiActivity.this);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(transaksiAdapter);
    }


    private void createPdf() throws FileNotFoundException, DocumentException {
        //isikan code createPdf()
        File docsFolder = new File(Environment.getExternalStorageDirectory() + "/Download/");
        if (!docsFolder.exists()) {
            docsFolder.mkdir();
            Log.i("TEST", "Direktori baru untuk file pdf berhasil dibuat");
        }


        String pdfname = "Laporan Transaksi" + ".pdf";
        pdfFile = new File(docsFolder.getAbsolutePath(), pdfname);
        OutputStream output = new FileOutputStream(pdfFile);
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(PageSize.A4);
        writer = PdfWriter.getInstance(document, output);
        document.open();

        Paragraph judul = new Paragraph(" Laporan Transaksi \n\n", new
                com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 16,
                com.itextpdf.text.Font.BOLD, BaseColor.BLACK));
        judul.setAlignment(Element.ALIGN_CENTER);
        document.add(judul);
        PdfPTable tables = new PdfPTable(new float[]{16, 8});
        tables.getDefaultCell().setFixedHeight(50);
        tables.setTotalWidth(PageSize.A4.getWidth());
        tables.setWidthPercentage(100);
        tables.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        PdfPCell cellSupplier = new PdfPCell();
        cellSupplier.setPaddingLeft(20);
        cellSupplier.setPaddingBottom(10);
        cellSupplier.setBorder(Rectangle.NO_BORDER);

        Paragraph Kepada = new Paragraph(
                "Kepada Yth : \n" + "Owner GMBKost" + "\n",
                new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 10,
                        com.itextpdf.text.Font.NORMAL, BaseColor.BLACK)
        );
        cellSupplier.addElement(Kepada);
        tables.addCell(cellSupplier);
        PdfPCell cellPO = new PdfPCell();

        Paragraph NomorTanggal = new Paragraph(
                "No : " + "1" + "\n\n" +
                        "Tanggal : " + "12 Desember 2020" + "\n",
                new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 10,
                        com.itextpdf.text.Font.NORMAL, BaseColor.BLACK)
        );
        NomorTanggal.setPaddingTop(5);
        tables.addCell(NomorTanggal);
        document.add(tables);
        com.itextpdf.text.Font f = new
                com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 10,
                com.itextpdf.text.Font.NORMAL, BaseColor.BLACK);
        Paragraph Pembuka = new Paragraph("\nBerikut adalah laporan transaksi penyewaan kost: \n\n", f);
        Pembuka.setIndentationLeft(20);
        document.add(Pembuka);
        PdfPTable tableHeader = new PdfPTable(new float[]{5, 5, 5, 5});
        tableHeader.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        tableHeader.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
        tableHeader.getDefaultCell().setFixedHeight(30);
        tableHeader.setTotalWidth(PageSize.A4.getWidth());
        tableHeader.setWidthPercentage(100);

        PdfPCell h1 = new PdfPCell(new Phrase("ID Transaksi"));
        h1.setHorizontalAlignment(Element.ALIGN_CENTER);
        h1.setPaddingBottom(5);
        PdfPCell h2 = new PdfPCell(new Phrase("ID Kost"));
        h2.setHorizontalAlignment(Element.ALIGN_CENTER);
        h2.setPaddingBottom(5);
        PdfPCell h3 = new PdfPCell(new Phrase("Durasi"));
        h3.setHorizontalAlignment(Element.ALIGN_CENTER);
        h3.setPaddingBottom(5);
        PdfPCell h4 = new PdfPCell(new Phrase("Total Bayar"));
        h4.setHorizontalAlignment(Element.ALIGN_CENTER);
        h4.setPaddingBottom(5);
        tableHeader.addCell(h1);
        tableHeader.addCell(h2);
        tableHeader.addCell(h3);
        tableHeader.addCell(h4);
        PdfPCell[] cells = tableHeader.getRow(0).getCells();
        for (int j = 0; j < cells.length; j++) {
            cells[j].setBackgroundColor(BaseColor.GRAY);
        }
        document.add(tableHeader);
        PdfPTable tableData = new PdfPTable(new float[]{5, 5, 5, 5});
        tableData.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
        tableData.getDefaultCell().setFixedHeight(30);
        tableData.setTotalWidth(PageSize.A4.getWidth());
        tableData.setWidthPercentage(100);
        tableData.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
        int arrLength = lsitTransaksi.size();

        for (int x = 1; x < arrLength; x++) {
            for (int i = 0; i < cells.length; i++) {
                if (i == 0) {
                    tableData.addCell(lsitTransaksi.get(x).getId());
                } else if (i == 1) {
                    tableData.addCell(lsitTransaksi.get(x).getId_kost());
                } else if(i == 2){
                    tableData.addCell(String.valueOf(lsitTransaksi.get(x).getLama_sewa()));
                }else {
                    tableData.addCell(String.valueOf(lsitTransaksi.get(x).getTotal_pembayaran()));
                }
            }
        }
        document.add(tableData);
        com.itextpdf.text.Font h = new
                com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 10,
                com.itextpdf.text.Font.NORMAL);
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
        String tglDicetak = sdf.format(currentTime);
        Paragraph P = new Paragraph("\nDicetak tanggal " + tglDicetak, h);
        P.setAlignment(Element.ALIGN_RIGHT);
        document.add(P);
        document.close();

        previewPdf();

    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.app.AlertDialog.Builder(ListTransaksiActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void createPdfWrapper() throws FileNotFoundException, DocumentException {
        //isikan code createPdfWrapper()
        int hasWriteStoragePermission = 0;
        if (hasWriteStoragePermission != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CONTACTS)) {
                    showMessageOKCancel("Izinkan aplikasi untuk akses penyimpanan?",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new
                                                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                REQUEST_CODE_ASK_PERMISSIONS);
                                    }
                                }
                            });
                    return;
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
            return;
        } else {
            createPdf();
        }

    }

    private void previewPdf() {
        //isikan code previewPdf()
        PackageManager packageManager = getPackageManager();
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        testIntent.setType("application/pdf");
        List list = packageManager.queryIntentActivities(testIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(ListTransaksiActivity.this, getPackageName() + ".provider",
                        pdfFile);
            } else {
                uri = Uri.fromFile(pdfFile);
            }
            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.setDataAndType(uri, "application/pdf");
            pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pdfIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            pdfIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            grantUriPermission("com.calvindo.aldi.sutanto.tubes", uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(pdfIntent);
        } else {
            Toast.makeText(ListTransaksiActivity.this, "Unduh pembuka PDF untuk menampilkan file ini", Toast.LENGTH_SHORT).show();
        }

    }







}