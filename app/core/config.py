from pydantic_settings import BaseSettings
#backendin ayar beyni
class Settings(BaseSettings):#basesettings env dosyasını güvenli bir şekilde okur
    # genel ayarları bir sınıf içinde toplar
    ENV: str = "dev" #şuanki uygulamanın nerde çalıştığını belirtir ve varsayılan olarak "dev" (geliştirme) olarak ayarlanır
    #"dev" → geliştirme
    #"prod" → üretim ortamı
    #"test" → test ortamı

    API_PREFIX: str = "/api" #api uç noktalarının ön eki ve varsayılan olarak "/api" olarak ayarlanır sebebi api uç noktalarını organize etmek ve son kullanıcıların bu uç noktalara erişimini kolaylaştırmaktır frontend ile backend arasındaki iletişimde kullanılır

    JWT_SECRET: str #neden JWT_SECRET tanımlandı çünkü jwt tokenlarının güvenliğini sağlamak için gereklidir burda herhangi bir varsayılan değer verilmemiştir çünkü bu değerin mutlaka .env dosyasından sağlanması gerekir. kimlik doğrulama ve yetkilendirme işlemlerinde kullanılır
    #token nedir? token, kullanıcıların kimlik doğrulama ve yetkilendirme işlemlerinde kullanılan dijital bir anahtardır. Genellikle kullanıcı oturumlarını yönetmek ve güvenli erişim sağlamak için kullanılırlar.
    JWT_EXPIRES_MIN: int = 60 #jwt tokenlarının geçerlilik süresi dakika cinsinden ve varsayılan olarak 60 dakika (1 saat) olarak ayarlanır çünkü güvenlik açısından tokenların belirli bir süre sonra geçersiz hale gelmesi önemlidir böylece çalınan tokenların uzun süreli kullanımının önüne geçilir.

    DATABASE_URL: str  # PostgreSQL bağlanmak için gerelekli bağlantı dizesi
    MONGODB_URI: str   # Mongo Tüm veritabanı işlemleri bu URL üzerinden yapılır.SQLAlchemy veya asyncpg bu adresle bağlanır. çünkü veritabanı bağlantı bilgileri gizli tutulmalıdır nasıl güvenli tutulur? .env dosyasında saklanarak
    MONGODB_DB: str # nedir? MongoDB veritabanının adı çünkü uygulamanın hangi veritabanını kullanacağını belirtir

    CORS_ORIGINS: str = "*" #Frontend (örneğin React, Flutter Web) hangi domain’den API’ye erişebilir, bunu belirler. Varsayılan olarak "*" (her yerden erişim) olarak ayarlanır ancak üretim ortamında güvenlik için belirli domainler ile sınırlandırılması önerilir.
    RATE_LIMIT: str = "60/minute" # API'ye yapılan isteklerin sınırlandırılması için kullanılır. Varsayılan olarak "60/minute" (dakikada 60 istek) olarak ayarlanır. Bu, aşırı yüklenmeyi önlemek ve hizmetin sürekliliğini sağlamak için önemlidir. DDoS (yüksek istek saldırısı) veya spam’i önler.

    class Config: # Config sınıfı, Pydantic'in BaseSettings sınıfının yapılandırma seçeneklerini belirlemek için kullanılır. ayarların nasıl okunacağını ve yükleneceğini tanımlar
        env_file = ".env" #basesettings env yi nereden okuyacağını söyler burada .env dosyasından okur. çünkü basesettings nereden ayarları alacağını bilmez.

settings = Settings() #uygulamanın her yerinde kullanılmak üzere Settings sınıfının bir örneğini oluşturur böylece ayarlara kolayca erişilebilir.
