package arcata.api.clients

import utest.*
import java.util.UUID
import scala.collection.mutable

object ObjectStorageClientSuite extends TestSuite:
  /**
   * In-memory implementation of ObjectStorageClient for testing.
   * Simulates the storage operations without making real HTTP calls.
   */
  class InMemoryObjectStorageClient extends ObjectStorageClient("http://test", "test"):
    private val storage = mutable.Map[String, (Array[Byte], StoredObject)]()

    override def upload(
        content: Array[Byte],
        fileName: String,
        mimeType: Option[String],
        userId: String
    ): Either[StorageError, StoredObject] = {
      val objectId = UUID.randomUUID().toString
      val obj = StoredObject(
        objectId = objectId,
        fileName = fileName,
        size = content.length,
        bucket = "test-bucket",
        lastModified = java.time.Instant.now().toString,
        createdAt = java.time.Instant.now().toString,
        mimeType = mimeType
      )
      storage(objectId) = (content, obj)
      Right(obj)
    }

    override def download(objectId: String, userId: String): Either[StorageError, Array[Byte]] =
      storage.get(objectId) match
        case Some((content, _)) => Right(content)
        case None => Left(StorageError.NotFound(objectId))

    override def getMetadata(objectId: String, userId: String): Either[StorageError, StoredObject] =
      storage.get(objectId) match
        case Some((_, obj)) => Right(obj)
        case None => Left(StorageError.NotFound(objectId))

    override def delete(objectId: String, userId: String): Either[StorageError, StoredObject] =
      storage.remove(objectId) match
        case Some((_, obj)) => Right(obj)
        case None => Left(StorageError.NotFound(objectId))

    def clear(): Unit = storage.clear()
    def size: Int = storage.size

  val tests = Tests {
    test("InMemoryObjectStorageClient") {
      test("upload stores content and returns metadata") {
        val client = InMemoryObjectStorageClient()
        val content = "Hello, World!".getBytes

        val result = client.upload(content, "test.txt", Some("text/plain"), "user-1")

        assert(result.isRight)
        result.foreach { obj =>
          assert(obj.fileName == "test.txt")
          assert(obj.size == content.length)
          assert(obj.mimeType == Some("text/plain"))
        }
      }

      test("download retrieves stored content") {
        val client = InMemoryObjectStorageClient()
        val content = "Test content".getBytes

        val uploadResult = client.upload(content, "file.txt", None, "user-1")
        assert(uploadResult.isRight)

        val objectId = uploadResult.toOption.get.objectId
        val downloadResult = client.download(objectId, "user-1")

        assert(downloadResult.isRight)
        downloadResult.foreach { bytes =>
          assert(bytes.sameElements(content))
        }
      }

      test("download returns NotFound for non-existent object") {
        val client = InMemoryObjectStorageClient()

        val result = client.download("non-existent-id", "user-1")

        assert(result.isLeft)
        result.left.foreach {
          case StorageError.NotFound(id) => assert(id == "non-existent-id")
          case other => throw new java.lang.AssertionError(s"Expected NotFound error, got $other")
        }
      }

      test("getMetadata returns object metadata") {
        val client = InMemoryObjectStorageClient()
        val content = "Some data".getBytes

        val uploadResult = client.upload(content, "data.bin", Some("application/octet-stream"), "user-1")
        val objectId = uploadResult.toOption.get.objectId

        val metadataResult = client.getMetadata(objectId, "user-1")

        assert(metadataResult.isRight)
        metadataResult.foreach { obj =>
          assert(obj.objectId == objectId)
          assert(obj.fileName == "data.bin")
        }
      }

      test("delete removes object and returns metadata") {
        val client = InMemoryObjectStorageClient()
        val content = "To be deleted".getBytes

        val uploadResult = client.upload(content, "delete-me.txt", None, "user-1")
        val objectId = uploadResult.toOption.get.objectId
        assert(client.size == 1)

        val deleteResult = client.delete(objectId, "user-1")

        assert(deleteResult.isRight)
        assert(client.size == 0)

        // Verify it's gone
        val downloadResult = client.download(objectId, "user-1")
        assert(downloadResult.isLeft)
      }

      test("delete returns NotFound for non-existent object") {
        val client = InMemoryObjectStorageClient()

        val result = client.delete("non-existent", "user-1")

        assert(result.isLeft)
        result.left.foreach {
          case StorageError.NotFound(_) => () // success
          case other => throw new java.lang.AssertionError(s"Expected NotFound error, got $other")
        }
      }

      test("clear removes all objects") {
        val client = InMemoryObjectStorageClient()
        client.upload("content1".getBytes, "file1.txt", None, "user-1")
        client.upload("content2".getBytes, "file2.txt", None, "user-1")
        assert(client.size == 2)

        client.clear()

        assert(client.size == 0)
      }
    }

    test("StoredObject") {
      test("serializes to JSON and back") {
        import upickle.default.*

        val obj = StoredObject(
          objectId = "abc-123",
          fileName = "test.txt",
          size = 1024L,
          bucket = "my-bucket",
          lastModified = "2024-01-01T00:00:00Z",
          createdAt = "2024-01-01T00:00:00Z",
          mimeType = Some("text/plain"),
          checksum = Some("sha256-hash")
        )

        val json = write(obj)
        val parsed = read[StoredObject](json)

        assert(parsed == obj)
      }
    }

    test("StorageError") {
      test("NotFound contains object ID") {
        val error = StorageError.NotFound("obj-123")
        error match
          case StorageError.NotFound(id) => assert(id == "obj-123")
          case _ => assert(false)
      }

      test("ApiError contains status code and message") {
        val error = StorageError.ApiError(500, "Internal error")
        error match
          case StorageError.ApiError(code, msg) =>
            assert(code == 500)
            assert(msg == "Internal error")
          case _ => assert(false)
      }

      test("NetworkError contains message and optional cause") {
        val cause = new RuntimeException("Connection failed")
        val error = StorageError.NetworkError("Network failure", Some(cause))
        error match
          case StorageError.NetworkError(msg, c) =>
            assert(msg == "Network failure")
            assert(c.isDefined)
          case _ => assert(false)
      }
    }
  }
