//import documents.DjvuDocument
//import universe.constellation.orion.viewer.Bitmap
//
//fun main(args: Array<String>) {
//    val djvuDocument = DjvuDocument("../orion-viewer/src/androidTest/assets/testData/aliceinw.djvu")
//    println(djvuDocument.pageCount)
//    val pageInfo = djvuDocument.getPageInfo(0, 0)
//    println(pageInfo)
//    val bitmap = Bitmap(2481, 3508)
//    //println(bitmap.pixels.sum())
//    println(bitmap.pixels.get().take(4).joinToString(","))
//    djvuDocument.renderPage(0, bitmap, 1.0, 0, 0, 2481, 3508)
//    println(bitmap.pixels.get().sum())
//    println(bitmap.pixels.get().take(4).joinToString(","))
//    //println(bitmap.pixels.joinToString(","))
//    djvuDocument.destroy()
//}