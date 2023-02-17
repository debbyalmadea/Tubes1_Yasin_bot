# Tubes1_Yasin_bot
Tugas Besar I IF2211 Strategi Algoritma Semester II Tahun 2021/2022

Pemanfaatan Algoritma Greedy dalam Aplikasi Permainan "Galaxio"

## Daftar Isi
* [Deskripsi Singkat Program](#deskripsi-singkat-tugas)
* [Strategi Greedy Program](#strategi-greedy-program)
* [Struktur Program](#struktur-program)
* [Requirement Program](#requirement-program)
* [Cara Kompilasi Program](#cara-kompilasi-program)
* [Cara Menjalankan Program](#cara-menjalankan-program)
* [Cara Menjalankan Visualiser](#cara-menjalankan-visualiser)
* [Author Program](#author-program)

## Deskripsi Singkat Tugas
Galaxio adalah sebuah game battle royale yang mempertandingkan bot kapal anda dengan beberapa bot kapal yang lain. Setiap pemain akan memiliki sebuah bot kapal dan tujuan dari permainan adalah agar bot kapal anda yang tetap hidup hingga akhir permainan. Penjelasan lebih lanjut mengenai aturan permainan akan dijelaskan di bawah. Agar dapat memenangkan pertandingan, setiap bot harus mengimplementasikan strategi tertentu untuk dapat memenangkan permainan.
Pada tugas besar pertama Strategi Algoritma ini, tugas mahasiswa adalah mengimplementasikan bot kapal dalam permainan Galaxio dengan menggunakan strategi greedy untuk memenangkan permainan. 

## Strategi Greedy Program
Dalam permainan Galaxio, tujuan setiap _bot_ adalah untuk bertahan hidup sampai akhir permainan. Tujuan ini tercapai dengan mempertahankan ukuran kapal yang besar dan menyerang kapal _bot_ lain. Banyak cara untuk meraih hal tersebut, seperti mengambil makanan sebanyak mungkin, menyerang musuh terus menerus, dan lain sebagainya.

## Struktur Program
```bash
├───.github
│   └───workflows
├───src
│   └───main
│       └───java
│           ├───Enums
│           ├───Models
│           └───Services
├───doc
│   └─── Yasin_bot.pdf
│
│
└───target
    ├───classes
    │   ├───Enums
    │   ├───Models
    │   └───Services
    ├───libs
    ├───maven-archiver
    ├───maven-status
    │   └───maven-compiler-plugin
    │       └───compile
    │           └───default-compile
    └───test-classes
```

## Requirement Program
1. Java Virtual Machine versi 11 atau lebih baru
2. IntelliJ IDEA
3. .Net Core 3.1 dan .Net Core 5.0
4. Apache Maven 3.8.7


## Cara Kompilasi Program
1. Download file `starter-pack.zip` pada link [berikut] (https://github.com/EntelectChallenge/2021-Galaxio/releases/tag/2021.3.2)
2. Unzip file `starter-pack.zip` pada mesin eksekusi
3. Lakukan cloning repositori ini sebagai folder ke dalam folder `starter-pack\starter-bots\JavaBot`
4. Pada terminal dengan directory starter-pack\starter-bots\JavaBot, input perintah `mvn clean package`
5. Bila terdapat file `.jar` baru pada folder `target`, maka program berhasil dikompilasi

## Cara Menjalankan Program
1. Pastikan directory program berada di `starter-pack`
2. Anda dapat menjalankan program dengan membuka file `run.bat` atau dengan input terminal `./run.bat`
3. Jika berhasil dan pertandingan selesai, akan ada 2 file baru pada folder `starter-pack\logger-publish`

## Cara Menjalankan Visualiser
1. Buka folder `starter-pack\visualiser`
2. Unzip folder `Galaxio-` dengan OS yang sesuai dengan perangkat yang sedang digunakan
3. Buka aplikasi Galaxio pada folder yang telah diunzip
4. Pada bagian `OPTIONS` ubah log file location menjadi directory folder `logger-publish` berada
5. Untuk menvisualisasikan pertandingan yang telah dijalankan, klik `LOAD` dan pilih file `.json` pertandingan yang ingin divisualisasikan dan klik `START`

## Author Program
* [Made Debby Almadea Putri - 13521153](https://github.com/debbyalmadea)
* [Muhammad Hanan - 13521041](https://github.com/tarsn)
* [Alex Sander - 13521061](https://github.com/maximatey)
