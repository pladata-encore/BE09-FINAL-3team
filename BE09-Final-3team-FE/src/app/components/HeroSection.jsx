import Image from "next/image";
import styles from "../styles/HeroSection.module.css";

export default function HeroSection() {
  const features = [
    {
      id: 1,
      badge: "Find Opportunities",
      title: "체험단",
      description:
        "반려동물 프로필에 맞춘 광고 체험단을 둘러보세요. 신청 절차는 간단하고, 체험단 진행 상황도 언제든지 확인할 수 있어서 처음 이용하시는 분들도 어렵지 않게 참여하실 수 있어요.",
      features: [
        "다양한 체험 활동",
        "간편한 신청 절차",
        "실시간 진행 상황 안내",
      ],
      image: "/campaign.png",
      color: "#F5A623",
      reverse: false,
    },
    {
      id: 2,
      badge: "Showcase Your Pets",
      title: "반려동물 관리",
      description:
        "우리 아이의 사진과 성격을 담은 프로필을 만들고, 특별한 포트폴리오도 완성해보세요!",
      features: [
        "다중 반려동물 프로필",
        "사진과 영상 포함한 맞춤 포트폴리오 생성",
        "소셜 미디어 연동",
      ],
      image: "/petcare.png",
      color: "#8BC34A",
      reverse: true,
    },
    {
      id: 3,
      badge: "Grow Your Audience",
      title: "SNS 관리",
      description:
      "반려동물 SNS 계정의 팔로워 증가, 참여율, 콘텐츠 반응을 분석하세요. 댓글도 쉽게 관리하고, 유해한 댓글은 자동으로 감지해드려요.",
      features: [
        "SNS 분석",
        "댓글 관리",
        "SNS 활동의 성과를 분석하고 성장 흐름을 파악",
      ],
      image: "/sns.png",
      color: "#60A5FA",
      reverse: false,
    },
    {
      id: 4,
      badge: "Keep Them Healthy",
      title: "건강관리",
      description:
        "반려동물의 건강 상태를 관리해보세요! 예방접종 스케줄, 일일 활동 기록, 건강 데이터 시각화까지 한눈에 확인하고, 필요한 알림도 받아볼 수 있어요.",
      features: [
        "활동 및 건강 기록",
        "예방접종 및 약 복용 알림",
        "영양 섭취 기록 및 맞춤 추천",
      ],
      image: "/health.png",
      color: "#FF7675",
      reverse: true,
    },
    {
      id: 5,
      badge: "Connect & Share",
      title: "커뮤니티",
      description:
        "반려동물 애호가들의 활기찬 커뮤니티에 참여하세요. 사진 및 정보를 공유하거나 궁금한 점을 묻는 등 다른 반려동물 주인 및 인플루언서들과 소통할 수 있습니다. 서로의 경험에서 배우며 함께 성장하세요.",
      features: ["정보공유 게시판", "Q&A 게시판"],
      image: "/community.png",
      color: "#C084FC",
      reverse: false,
    },
  ];

  return (
    <section className={styles.heroSection}>
  <div className="container">
    {features.map((feature, idx) => (
      <div
        key={feature.id}
        className={`${styles.featureRow} ${feature.reverse ? styles.reverse : ""}`}
        data-aos="fade-up"
        data-aos-delay={idx * 120}
        data-aos-duration="800"
      >
        <div className={styles.contentArea}>
          <div className={styles.textContent}>
            <span
              className={styles.badge}
              style={{ color: feature.color }}
              data-aos="fade-up"
              data-aos-delay={100}
              data-aos-duration="500"
            >
              {feature.badge}
            </span>
            <h2
              className={styles.title}
              data-aos="fade-up"
              data-aos-delay={200}
              data-aos-duration="500"
            >
              {feature.title}
            </h2>
            <p
              className={styles.description}
              data-aos="fade-up"
              data-aos-delay={300}
              data-aos-duration="500"
            >
              {feature.description}
            </p>
            <ul className={styles.featureList}>
              {feature.features.map((item, index) => (
                <li
                  key={index}
                  className={styles.featureItem}
                  data-aos="fade-up"
                  data-aos-delay={400 + index * 150}
                  data-aos-duration="500"
                >
                  <div className={styles.checkIcon}>
                    <svg width="14" height="10" viewBox="0 0 14 10" fill="none">
                      <path
                        d="M1 5L5 9L13 1"
                        stroke={feature.color}
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </div>
                  {item}
                </li>
              ))}
            </ul>
          </div>
        </div>
        <div
          className={styles.imageArea}
          data-aos="zoom-in"
          data-aos-delay={150}
          data-aos-duration="900"
        >
          <div className={styles.imageContainer}>
            <Image
              src={feature.image}
              alt={feature.title}
              width={624}
              height={624}
              className={styles.featureImage}
            />
          </div>
        </div>
      </div>
    ))}
  </div>
</section>





  );
}
