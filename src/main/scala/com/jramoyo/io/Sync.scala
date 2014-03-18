package com.jramoyo.io

import java.io.File

import scala.collection.JavaConverters.asScalaBufferConverter

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session

class Sync(jSch: JSch, auth: Auth, localDir: String, remoteDir: String) {

  val session: Session = jSch.getSession(auth.username, auth.host)
  session.setPassword(auth.password)
  session.connect()

  val channel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
  channel.connect()

  def start() = {
    val filesToAdd = toAdd(localFiles, remoteFiles);
    val filesToDelete = toDelete(localFiles, remoteFiles);

    filesToDelete.foreach(fileInfo => {
      val tragetPath = path(remoteDir, fileInfo)
      println("Deleting '%s' ...".format(tragetPath))
      channel.rm(tragetPath)
    })

    filesToAdd.foreach(fileInfo => {
      // from : List(a, b, c, d)
      // to   : List(List(a), List(a, b), List(a, b, c), List(a, b, c, d))
      val targetDirs = (remoteDir :: fileInfo.parents.tail).inits.toList.reverse.tail
      targetDirs.foreach(dir => {
        try {
          channel.mkdir(dir.mkString("/"))
        } catch {
          case e: Exception => println("Directory '%s' already exists".format(dir))
        }
      })

      val srcPath = path(localDir, fileInfo)
      val tragetPath = path(remoteDir, fileInfo)
      println("Adding '%s' to '%s'".format(srcPath, tragetPath))
      channel.put(srcPath, tragetPath, ChannelSftp.OVERWRITE)
    })

    def path(root: String, fileInfo: FileInfo) = {
      "%s/%s".format(root, fileInfo.path)
    }
  }

  def stop = {
    channel.exit();
    session.disconnect();
  }

  protected[io] def toAdd(srcFiles: List[FileInfo], targetFiles: List[FileInfo]): List[FileInfo] = {
    srcFiles.filter(fileInfo => !targetFiles.contains(fileInfo))
  }

  protected[io] def toDelete(srcFiles: List[FileInfo], targetFiles: List[FileInfo]): List[FileInfo] = {
    targetFiles.filter(fileInfo => !srcFiles.contains(fileInfo))
  }

  def localFiles(): List[FileInfo] = {
    val dir = _locaDir
    if (dir.isDirectory()) {
      val dirFiles = dir.listFiles().toList
      processLocalDirectory(dirFiles, List(localDir))
    } else {
      throw new Exception("'%s' is not a directory");
    }
  }

  // Factory method to enable mocking
  protected[io] def _locaDir = {
    new File(localDir)
  }

  def remoteFiles(): List[FileInfo] = {
    try {
      val dirEntries = channel.ls(remoteDir).asScala.toList
      processRemoteDirectory(dirEntries, List(remoteDir))
    } catch {
      case e: Exception => {
        println("Unable to fetch contents of remote directory '%s'".format(remoteDir))
        List()
      }
    }
  }

  protected[io] def processRemoteFile(channel: ChannelSftp, parents: List[String], entry: Object): List[FileInfo] = {
    val lsEntry = entry.asInstanceOf[channel.LsEntry]
    if (lsEntry.getAttrs().isDir()) {
      println("Processing remote directory '%s'".format(lsEntry.getFilename()))
      val dirEntries = channel.ls(lsPath(parents, lsEntry.getFilename())).asScala.toList
      processRemoteDirectory(dirEntries, parents :+ lsEntry.getFilename())
    } else {
      val fileInfo = FileInfo(remoteDir, parents, lsEntry.getFilename(), lsEntry.getAttrs().getSize())
      println("Processed remote file '%s'".format(fileInfo.path))
      List(fileInfo)
    }
  }

  protected[io] def processLocalFile(parents: List[String], file: File): List[FileInfo] = {
    if (file.isDirectory()) {
      println("Processing local directory '%s'".format(file.getName()))
      val dirFiles = file.listFiles().toList
      processLocalDirectory(dirFiles, parents :+ file.getName())
    } else {
      val fileInfo = FileInfo(localDir, parents, file.getName(), file.length())
      println("Processed local file '%s'".format(fileInfo.path))
      List(fileInfo)
    }
  }

  private def lsPath(parents: List[String], fileName: String): String = {
    if (!parents.isEmpty) {
      "%s/%s".format(parents.mkString("/"), fileName)
    } else {
      fileName
    }
  }

  private def processRemoteDirectory(dirEntries: List[Any], parents: List[String]): List[FileInfo] = {
    dirEntries
      .filter(dirEntry => !dirEntry.asInstanceOf[channel.LsEntry].getFilename().startsWith("."))
      .map(dirEntry => {
        processRemoteFile(channel, parents, dirEntry.asInstanceOf[channel.LsEntry])
      }).flatten
  }

  private def processLocalDirectory(dirFiles: List[java.io.File], parents: List[String]): List[FileInfo] = {
    dirFiles.map(dirFile => processLocalFile(parents, dirFile)).flatten
  }
}

class Auth(val host: String, val username: String, val password: String)

object Auth {
  def apply(host: String, username: String, password: String): Auth = {
    new Auth(host, username, password);
  }
}

class FileInfo(root: String, val parents: List[String], val name: String, val size: Long) extends Equals {
  def path = {
    if (parents.isEmpty || (parents.size == 1 && parents(0) == root)) {
      name
    } else {
      val parentPath = if (parents(0) == root) {
        parents.tail.mkString("/")
      } else {
        parents.mkString("/")
      }

      "%s/%s".format(parentPath, name)
    }
  }

  def canEqual(other: Any) = {
    other.isInstanceOf[com.jramoyo.io.FileInfo]
  }

  override def equals(other: Any) = {
    other match {
      case that: com.jramoyo.io.FileInfo => that.canEqual(FileInfo.this) && path == that.path && size == that.size
      case _ => false
    }
  }

  override def hashCode() = {
    val prime = 41
    prime * (prime + path.hashCode) + size.hashCode
  }
}

object FileInfo {
  def apply(root: String, parents: List[String], name: String, size: Long): FileInfo = {
    new FileInfo(root, parents, name, size);
  }
}