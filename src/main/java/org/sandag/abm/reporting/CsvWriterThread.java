package org.sandag.abm.reporting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.BlockingQueue;
import org.apache.log4j.Logger;

public class CsvWriterThread
        implements Runnable
{
    private final BlockingQueue<CsvRow> queue;
    private final File                  file;
    private final String[]              header;

    private static final Logger         LOGGER     = Logger.getLogger(CsvWriterThread.class);

    private static final int            MAX_BUFFER = 1024 * 1024 * 1024;
    private static final String         ENCODING   = "UTF-8";

    public CsvWriterThread(BlockingQueue<CsvRow> aRowQueue, File anOutputLocation, String[] aHeader)
    {
        this.queue = aRowQueue;
        this.file = anOutputLocation;
        this.header = aHeader;
    }

    // @Override
    // public void run()
    // {
    // RandomAccessFile raf = null;
    // try
    // {
    // raf = new RandomAccessFile(file, "rw");
    // FileChannel outChannel = raf.getChannel();
    // int position = 0;
    // ByteBuffer buffer = outChannel.map(MapMode.READ_WRITE, position,
    // MAX_BUFFER);
    //
    // CsvRow headerRow = new CsvRow(header);
    // buffer.put(headerRow.getRow().getBytes(ENCODING));
    //
    // CsvRow row = null;
    // while ((row = queue.take()) != TruckCsvExporter.POISON_PILL)
    // {
    // byte[] rowBytes = row.getRow().getBytes(ENCODING);
    // if ((buffer.position() + rowBytes.length) > buffer.capacity())
    // {
    // position += buffer.position();
    // buffer = outChannel.map(MapMode.READ_WRITE, position, MAX_BUFFER);
    // }
    // buffer.put(rowBytes);
    // }
    // LOGGER.info("POISON Pill found. Clearing Buffer and Writing Remains.");
    // position += buffer.position();
    // buffer = null;
    // outChannel.truncate(position+1);
    // } catch (IOException ioe)
    // {
    //
    // } catch (InterruptedException e)
    // {
    // } finally
    // {
    // if (null != raf) try
    // {
    // raf.close();
    // LOGGER.info("Truck Writer Stream Closed.");
    // } catch (IOException e)
    // {
    // LOGGER.error(e);
    // }
    // }
    // }

    @Override
    public void run()
    {
        FileOutputStream outStream = null;
        try
        {
            outStream = new FileOutputStream(file, false);
            FileChannel outChannel = outStream.getChannel();
            ByteBuffer buffer = ByteBuffer.allocateDirect(MAX_BUFFER);

            CsvRow headerRow = new CsvRow(header);
            buffer.put(headerRow.getRow().getBytes(ENCODING));

            CsvRow row = null;
            while ((row = queue.take()) != TruckCsvExporter.POISON_PILL)
            {
                byte[] rowBytes = row.getRow().getBytes(ENCODING);
                if ((buffer.position() + rowBytes.length) > buffer.capacity())
                {
                    buffer.flip();
                    outChannel.write(buffer);
                    buffer.clear();
                }
                buffer.put(rowBytes);
            }
            LOGGER.info("POISON Pill found. Clearing Buffer and Writing Remains.");
            buffer.flip();
            outChannel.write(buffer);
        } catch (IOException ioe)
        {

        } catch (InterruptedException e)
        {
        } finally
        {
            if (null != outStream) try
            {
                outStream.close();
                LOGGER.info("Truck Writer Stream Closed.");
            } catch (IOException e)
            {
                LOGGER.error(e);
            }
        }
    }
}
