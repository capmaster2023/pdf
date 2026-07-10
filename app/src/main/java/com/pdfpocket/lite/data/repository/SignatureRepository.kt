package com.pdfpocket.lite.data.repository

import android.content.Context
import com.pdfpocket.lite.data.local.SignatureDao
import com.pdfpocket.lite.data.local.SignatureEntity
import com.pdfpocket.lite.security.KeystoreCrypto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignatureRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: SignatureDao,
    private val crypto: KeystoreCrypto
) {
    fun observe(): Flow<List<SignatureEntity>> = dao.observeAll()

    suspend fun save(name: String, png: ByteArray): Long {
        require(png.isNotEmpty())
        val fileName = "signature_${System.nanoTime()}.bin"
        val dir = File(context.filesDir, "signatures").apply { mkdirs() }
        File(dir, fileName).writeBytes(crypto.encrypt(png))
        return dao.insert(SignatureEntity(name = name, encryptedFileName = fileName, createdAt = System.currentTimeMillis()))
    }

    suspend fun bytes(id: Long): ByteArray {
        val entity = requireNotNull(dao.find(id))
        return crypto.decrypt(File(File(context.filesDir, "signatures"), entity.encryptedFileName).readBytes())
    }

    suspend fun delete(entity: SignatureEntity) {
        File(File(context.filesDir, "signatures"), entity.encryptedFileName).delete()
        dao.delete(entity)
    }

    suspend fun deleteAll() {
        File(context.filesDir, "signatures").deleteRecursively()
        dao.deleteAll()
    }
}
