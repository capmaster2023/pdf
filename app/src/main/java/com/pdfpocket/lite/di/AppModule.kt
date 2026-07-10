package com.pdfpocket.lite.di

import android.content.Context
import androidx.room.Room
import com.pdfpocket.lite.data.local.*
import com.pdfpocket.lite.data.repository.DocumentRepositoryImpl
import com.pdfpocket.lite.domain.DocumentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pdf_pocket_lite.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun documentDao(db: AppDatabase): DocumentDao = db.documentDao()
    @Provides fun operationDao(db: AppDatabase): OperationDao = db.operationDao()
    @Provides fun signatureDao(db: AppDatabase): SignatureDao = db.signatureDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDocumentRepository(implementation: DocumentRepositoryImpl): DocumentRepository
}
