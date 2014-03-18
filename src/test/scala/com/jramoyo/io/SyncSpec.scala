package com.jramoyo.io

import java.io.File
import java.util.Vector

import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpATTRS

class SyncSpec extends FlatSpec with Matchers with BeforeAndAfterEach {
  val jSch = mock(classOf[JSch])
  val channel = mock(classOf[ChannelSftp])
  val session = mock(classOf[Session])

  var sync: Sync = null;

  override def beforeEach() {
    when(session.openChannel("sftp")) thenReturn (channel)
    when(jSch.getSession("username", "host")) thenReturn (session)
    sync = new Sync(jSch, Auth("host", "username", "password"), "src", "target")
  }

  "processRemoteFile on a file" should "return a list of size 1" in {
    val lsEntry = mock(classOf[channel.LsEntry])
    val attrs = mock(classOf[SftpATTRS])

    when(lsEntry.getFilename()) thenReturn ("file.txt")
    when(lsEntry.getAttrs()) thenReturn (attrs)
    when(attrs.isDir()) thenReturn (false);
    when(attrs.getSize()) thenReturn (1L);

    val files = sync.processRemoteFile(channel, List("target"), lsEntry)

    files.size should equal(1)
    files(0).path should equal("file.txt")
    files(0).size should equal(1L)
  }

  "processRemoteFile on a directory" should "return a list of files" in {
    // File 1
    val file1 = mock(classOf[channel.LsEntry])
    val fileAttrs1 = mock(classOf[SftpATTRS])
    when(file1.getFilename()) thenReturn ("file1.txt")
    when(file1.getAttrs()) thenReturn (fileAttrs1)
    when(fileAttrs1.isDir()) thenReturn (false);
    when(fileAttrs1.getSize()) thenReturn (1L);

    // File 2
    val file2 = mock(classOf[channel.LsEntry])
    val fileAttrs2 = mock(classOf[SftpATTRS])
    when(file2.getFilename()) thenReturn ("file2.txt")
    when(file2.getAttrs()) thenReturn (fileAttrs2)
    when(fileAttrs2.isDir()) thenReturn (false);
    when(fileAttrs2.getSize()) thenReturn (2L);

    // Current Directory
    val currDir = mock(classOf[channel.LsEntry])
    val currDirAttrs = mock(classOf[SftpATTRS])
    when(currDir.getFilename()) thenReturn (".")
    when(currDir.getAttrs()) thenReturn (currDirAttrs)
    when(currDirAttrs.isDir()) thenReturn (true);
    when(currDirAttrs.getSize()) thenReturn (0L);

    // Current Directory
    val upDir = mock(classOf[channel.LsEntry])
    val upDirAttrs = mock(classOf[SftpATTRS])
    when(upDir.getFilename()) thenReturn ("..")
    when(upDir.getAttrs()) thenReturn (upDirAttrs)
    when(upDirAttrs.isDir()) thenReturn (true);
    when(upDirAttrs.getSize()) thenReturn (0L);

    // Directory
    val dir = mock(classOf[channel.LsEntry])
    val dirAttrs = mock(classOf[SftpATTRS])
    when(dir.getFilename()) thenReturn ("dir")
    when(dir.getAttrs()) thenReturn (dirAttrs)
    when(dirAttrs.isDir()) thenReturn (true);
    when(dirAttrs.getSize()) thenReturn (0L);

    val dirEntries = new Vector[channel.LsEntry]();
    dirEntries.add(file1);
    dirEntries.add(file2);
    dirEntries.add(currDir);
    dirEntries.add(upDir);
    doReturn(dirEntries).when(channel).ls("target/dir")

    val files = sync.processRemoteFile(channel, List("target"), dir)

    files.size should equal(2)

    files(0).path should equal("dir/file1.txt")
    files(0).size should equal(1L)

    files(1).path should equal("dir/file2.txt")
    files(1).size should equal(2L)
  }

  "remoteFiles" should "return a recursive list of files" in {
    // File 1
    val file1 = mock(classOf[channel.LsEntry])
    val fileAttrs1 = mock(classOf[SftpATTRS])
    when(file1.getFilename()) thenReturn ("file1.txt")
    when(file1.getAttrs()) thenReturn (fileAttrs1)
    when(fileAttrs1.isDir()) thenReturn (false);
    when(fileAttrs1.getSize()) thenReturn (1L);

    // File 2
    val file2 = mock(classOf[channel.LsEntry])
    val fileAttrs2 = mock(classOf[SftpATTRS])
    when(file2.getFilename()) thenReturn ("file2.txt")
    when(file2.getAttrs()) thenReturn (fileAttrs2)
    when(fileAttrs2.isDir()) thenReturn (false);
    when(fileAttrs2.getSize()) thenReturn (2L);

    // File 3
    val file3 = mock(classOf[channel.LsEntry])
    val fileAttrs3 = mock(classOf[SftpATTRS])
    when(file3.getFilename()) thenReturn ("file3.txt")
    when(file3.getAttrs()) thenReturn (fileAttrs3)
    when(fileAttrs3.isDir()) thenReturn (false);
    when(fileAttrs3.getSize()) thenReturn (3L);

    // Nested Directory
    val dir1 = mock(classOf[channel.LsEntry])
    val dirAttrs1 = mock(classOf[SftpATTRS])
    when(dir1.getFilename()) thenReturn ("dir")
    when(dir1.getAttrs()) thenReturn (dirAttrs1)
    when(dirAttrs1.isDir()) thenReturn (true);
    when(dirAttrs1.getSize()) thenReturn (0L);

    // Current Directory
    val currDir = mock(classOf[channel.LsEntry])
    val currDirAttrs = mock(classOf[SftpATTRS])
    when(currDir.getFilename()) thenReturn (".")
    when(currDir.getAttrs()) thenReturn (currDirAttrs)
    when(currDirAttrs.isDir()) thenReturn (true);
    when(currDirAttrs.getSize()) thenReturn (0L);

    // Current Directory
    val upDir = mock(classOf[channel.LsEntry])
    val upDirAttrs = mock(classOf[SftpATTRS])
    when(upDir.getFilename()) thenReturn ("..")
    when(upDir.getAttrs()) thenReturn (upDirAttrs)
    when(upDirAttrs.isDir()) thenReturn (true);
    when(upDirAttrs.getSize()) thenReturn (0L);

    val dirEntries1 = new Vector[channel.LsEntry]();
    dirEntries1.add(file3);
    doReturn(dirEntries1).when(channel).ls("target/dir")

    // Directory
    val dir = mock(classOf[channel.LsEntry])
    val dirAttrs = mock(classOf[SftpATTRS])
    when(dir.getFilename()) thenReturn ("target")
    when(dir.getAttrs()) thenReturn (dirAttrs)
    when(dirAttrs.isDir()) thenReturn (true);
    when(dirAttrs.getSize()) thenReturn (0L);

    val dirEntries = new Vector[channel.LsEntry]();
    dirEntries.add(file1);
    dirEntries.add(file2);
    dirEntries.add(dir1);
    dirEntries.add(currDir);
    dirEntries.add(upDir);
    doReturn(dirEntries).when(channel).ls("target")

    val files = sync.remoteFiles

    files.size should equal(3)

    files(0).path should equal("file1.txt")
    files(0).size should equal(1L)

    files(1).path should equal("file2.txt")
    files(1).size should equal(2L)

    files(2).path should equal("dir/file3.txt")
    files(2).size should equal(3L)
  }

  "targetFiles" should "return an empty list" in {
    doThrow(new RuntimeException()).when(channel).ls("target")

    val files = sync.remoteFiles

    files.isEmpty should equal(true)
  }

  "processLocalFile on a file" should "return a list of size 1" in {
    val file = mock(classOf[File])

    when(file.isDirectory()) thenReturn (false)
    when(file.getName()) thenReturn ("file.txt")
    when(file.length()) thenReturn (1L)

    val files = sync.processLocalFile(List("src"), file)

    files.size should equal(1)
    files(0).path should equal("file.txt")
    files(0).size should equal(1L)
  }

  "processLocalFile on a directory" should "return a list of files" in {
    // File 1
    val file1 = mock(classOf[File])
    when(file1.isDirectory()) thenReturn (false)
    when(file1.getName()) thenReturn ("file1.txt")
    when(file1.length()) thenReturn (1L)

    // File 2
    val file2 = mock(classOf[File])
    when(file2.isDirectory()) thenReturn (false)
    when(file2.getName()) thenReturn ("file2.txt")
    when(file2.length()) thenReturn (2L)

    // Directory
    val dir = mock(classOf[File])
    when(dir.isDirectory()) thenReturn (true)
    when(dir.getName()) thenReturn ("dir")
    when(dir.length()) thenReturn (0L)
    when(dir.listFiles()) thenReturn (Array(file1, file2))

    val files = sync.processLocalFile(List("src"), dir)

    files.size should equal(2)

    files(0).path should equal("dir/file1.txt")
    files(0).size should equal(1L)

    files(1).path should equal("dir/file2.txt")
    files(1).size should equal(2L)
  }

  "localFiles" should "return a recursive list of files" in {
    // File 1
    val file1 = mock(classOf[File])
    when(file1.isDirectory()) thenReturn (false)
    when(file1.getName()) thenReturn ("file1.txt")
    when(file1.length()) thenReturn (1L)

    // File 2
    val file2 = mock(classOf[File])
    when(file2.isDirectory()) thenReturn (false)
    when(file2.getName()) thenReturn ("file2.txt")
    when(file2.length()) thenReturn (2L)

    // File 3
    val file3 = mock(classOf[File])
    when(file3.isDirectory()) thenReturn (false)
    when(file3.getName()) thenReturn ("file3.txt")
    when(file3.length()) thenReturn (3L)

    // Nested Directory
    val dir1 = mock(classOf[File])
    when(dir1.isDirectory()) thenReturn (true)
    when(dir1.getName()) thenReturn ("dir")
    when(dir1.length()) thenReturn (0L)
    when(dir1.listFiles()) thenReturn (Array(file3))

    // Directory
    val dir = mock(classOf[File])
    when(dir.isDirectory()) thenReturn (true)
    when(dir.getName()) thenReturn ("src")
    when(dir.length()) thenReturn (0L)
    when(dir.listFiles()) thenReturn (Array(file1, file2, dir1))

    // Mocked Factory
    val syncSpy = spy(sync)
    when(syncSpy._locaDir) thenReturn (dir)

    val files = syncSpy.localFiles

    files.size should equal(3)

    files(0).path should equal("file1.txt")
    files(0).size should equal(1L)

    files(1).path should equal("file2.txt")
    files(1).size should equal(2L)

    files(2).path should equal("dir/file3.txt")
    files(2).size should equal(3L)
  }

  "srcFiles" should "throw an exception" in {
    val file = mock(classOf[File])
    when(file.isDirectory()) thenReturn (false)

    // Mocked Factory
    val syncSpy = spy(sync)
    when(syncSpy._locaDir) thenReturn (file)

    intercept[Exception] {
      syncSpy.localFiles
    }
  }

  "toDelete" should "return a list of files to delete" in {
    val srcFiles = List(
      FileInfo("src", List("src"), "file1.txt", 1L),
      FileInfo("src", List("src"), "file2.txt", 2L),
      FileInfo("src", List("src"), "file3.txt", 3L))

    val targetFiles = List(
      FileInfo("target", List("target"), "file3.txt", 3L),
      FileInfo("target", List("target"), "file4.txt", 4L),
      FileInfo("target", List("target"), "file5.txt", 5L))

    val files = sync.toDelete(srcFiles, targetFiles);

    files.size should equal(2)

    files(0).path should equal("file4.txt")
    files(0).size should equal(4L)

    files(1).path should equal("file5.txt")
    files(1).size should equal(5L)
  }

  "toAdd" should "return a list of files to add" in {
    val srcFiles = List(
      FileInfo("src", List("src"), "file1.txt", 1L),
      FileInfo("src", List("src"), "file2.txt", 2L),
      FileInfo("src", List("src"), "file3.txt", 3L),
      FileInfo("src", List("src"), "file4.txt", 4L),
      FileInfo("src", List("src"), "file5.txt", 5L))

    val targetFiles = List(
      FileInfo("target", List("target"), "file1.txt", 1L),
      FileInfo("target", List("target"), "file2.txt", 2L),
      FileInfo("target", List("target"), "file3.txt", 0L))

    val files = sync.toAdd(srcFiles, targetFiles);

    files.size should equal(3)

    files(0).path should equal("file3.txt")
    files(0).size should equal(3L)

    files(1).path should equal("file4.txt")
    files(1).size should equal(4L)

    files(2).path should equal("file5.txt")
    files(2).size should equal(5L)
  }

  "start" should "sync the source directory against the target directory" in {
    val srcFiles = List(
      FileInfo("src", List("src"), "file1.txt", 1L),
      FileInfo("src", List("src"), "file2.txt", 2L),
      FileInfo("src", List("src"), "file3.txt", 3L),
      FileInfo("src", List("src"), "file4.txt", 4L),
      FileInfo("src", List("src", "dir"), "file5.txt", 5L))

    val targetFiles = List(
      FileInfo("target", List("target"), "file1.txt", 1L),
      FileInfo("target", List("target"), "file2.txt", 2L),
      FileInfo("target", List("target"), "file3.txt", 0L),
      FileInfo("target", List("target"), "file6.txt", 6L),
      FileInfo("target", List("target", "dir"), "file7.txt", 7L))

    val syncSpy = spy(sync)
    doReturn(srcFiles).when(syncSpy).localFiles
    doReturn(targetFiles).when(syncSpy).remoteFiles

    syncSpy.start;

    verify(channel, times(3)).mkdir("target")
    verify(channel, times(1)).mkdir("target/dir")

    verify(channel, times(1)).rm("target/file6.txt")
    verify(channel, times(1)).rm("target/file3.txt")
    verify(channel, times(1)).rm("target/dir/file7.txt")

    verify(channel, times(1)).put("src/file3.txt", "target/file3.txt", ChannelSftp.OVERWRITE)
    verify(channel, times(1)).put("src/file4.txt", "target/file4.txt", ChannelSftp.OVERWRITE)
    verify(channel, times(1)).put("src/dir/file5.txt", "target/dir/file5.txt", ChannelSftp.OVERWRITE)
  }

  "stop" should "close connections" in {
    sync.stop;

    verify(channel, times(1)).exit()
    verify(session, times(1)).disconnect()
  }
}