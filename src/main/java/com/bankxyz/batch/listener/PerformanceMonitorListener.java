package com.bankxyz.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Listener para monitorear el rendimiento de procesamiento
 * Proporciona métricas en tiempo real de los jobs independientes
 */
@Component
public class PerformanceMonitorListener<T, S> implements 
        ItemReadListener<T>, 
        ItemProcessListener<T, S>, 
        ItemWriteListener<S> {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitorListener.class);
    
    private final AtomicLong readCount = new AtomicLong(0);
    private final AtomicLong processCount = new AtomicLong(0);
    private final AtomicLong writeCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong skippedCount = new AtomicLong(0);
    
    private long startTime;

    // Read Listeners
    @Override
    public void beforeRead() {
        if (readCount.get() == 0) {
            startTime = System.currentTimeMillis();
            logger.info("🚀 Iniciando procesamiento de archivo CSV...");
        }
    }

    @Override
    public void afterRead(T item) {
        long count = readCount.incrementAndGet();
        
        // Log cada 100 registros para monitoreo
        if (count % 100 == 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            double rate = count / (elapsed / 1000.0);
            logger.info("📊 Progreso: {} registros leídos - Rate: {:.1f} reg/seg", count, rate);
        }
    }

    @Override
    public void onReadError(Exception ex) {
        long errors = errorCount.incrementAndGet();
        logger.warn("❌ Error de lectura #{}: {}", errors, ex.getMessage());
    }

    // Process Listeners
    @Override
    public void beforeProcess(T item) {
        // No logging aquí para evitar spam
    }

    @Override
    public void afterProcess(T item, S result) {
        processCount.incrementAndGet();
        
        if (result == null) {
            skippedCount.incrementAndGet();
        }
    }

    @Override
    public void onProcessError(T item, Exception e) {
        long errors = errorCount.incrementAndGet();
        logger.warn("⚠️ Error de procesamiento #{}: {} - Item: {}", errors, e.getMessage(), item);
    }

    // Write Listeners
    @Override
    public void beforeWrite(Chunk<? extends S> items) {
        // No logging aquí para evitar spam
    }

    @Override
    public void afterWrite(Chunk<? extends S> items) {
        long count = writeCount.addAndGet(items.size());
        
        // Log cada chunk escrito
        if (count % 50 == 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            double rate = count / (elapsed / 1000.0);
            logger.info("💾 Escritura: {} registros guardados - Rate: {:.1f} reg/seg", count, rate);
        }
    }

    @Override
    public void onWriteError(Exception exception, Chunk<? extends S> items) {
        long errors = errorCount.incrementAndGet();
        logger.error("💥 Error de escritura #{}: {} - Chunk size: {}", 
                    errors, exception.getMessage(), items.size());
    }

    /**
     * Método para obtener estadísticas finales
     */
    public void logFinalStats(String jobName) {
        long elapsed = System.currentTimeMillis() - startTime;
        double elapsedSeconds = elapsed / 1000.0;
        
        logger.info("📈 ESTADÍSTICAS FINALES - Job: {}", jobName);
        logger.info("⏱️  Tiempo total: {:.2f} segundos", elapsedSeconds);
        logger.info("📖 Registros leídos: {}", readCount.get());
        logger.info("🔄 Registros procesados: {}", processCount.get());
        logger.info("💾 Registros escritos: {}", writeCount.get());
        logger.info("⏭️  Registros omitidos: {}", skippedCount.get());
        logger.info("❌ Errores totales: {}", errorCount.get());
        
        if (elapsedSeconds > 0 && writeCount.get() > 0) {
            double throughput = writeCount.get() / elapsedSeconds;
            logger.info("🚀 Throughput promedio: {:.1f} registros/segundo", throughput);
        }
        
        if (readCount.get() > 0) {
            double successRate = (writeCount.get() * 100.0) / readCount.get();
            logger.info("✅ Tasa de éxito: {:.1f}%", successRate);
        }
        
        resetCounters();
    }
    
    private void resetCounters() {
        readCount.set(0);
        processCount.set(0);
        writeCount.set(0);
        errorCount.set(0);
        skippedCount.set(0);
    }
    
    // Getters para métricas
    public long getReadCount() { return readCount.get(); }
    public long getProcessCount() { return processCount.get(); }
    public long getWriteCount() { return writeCount.get(); }
    public long getErrorCount() { return errorCount.get(); }
    public long getSkippedCount() { return skippedCount.get(); }
}