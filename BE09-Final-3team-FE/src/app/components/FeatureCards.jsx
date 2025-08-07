import Link from "next/link";
import Image from "next/image";
import { FiGift, FiHeart, FiActivity, FiShare2, FiUsers } from "react-icons/fi";
import styles from "../styles/FeatureCards.module.css";

export default function FeatureCards() {
  const cards = [
    {
      id: 1,
      icon: (
        <div className={styles.iconWrapper}>
          <FiGift size={36} color="#F5A623" />
        </div>
      ),
      title: "체험단",
      description: "브랜드와 연결하여 반려동물의 영향력을 수익화하세요",
      color: "#F5A623",
      link: "/campaign",
    },
    {
      id: 2,
      icon: (
        <div className={styles.iconWrapper}>
          <FiHeart size={36} color="#8BC34A" />
        </div>
      ),
      title: "펫 관리",
      description: "모든 반려동물을 위한 프로필 및 포트폴리오 만들기",
      color: "#8BC34A",
      link: "/user/management",
    },
    {
      id: 4,
      icon: (
        <div className={styles.iconWrapper}>
          <FiShare2 size={36} color="#60A5FA" />
        </div>
      ),
      title: "SNS 관리",
      description: "반려동물의 소셜 미디어 활동을 분석하고 성장시켜보세요.",
      color: "#60A5FA",
      link: "/sns",
    },
    {
      id: 3,
      icon: (
        <div className={styles.iconWrapper}>
          <FiActivity size={36} color="#FF7675" />
        </div>
      ),
      title: "건강관리",
      description:
        "반려동물의 건강 상태를 추적하고 일상을 체계적으로 관리해보세요.",
      color: "#FF7675",
      link: "/health/activity",
    },

    {
      id: 5,
      icon: (
        <div className={styles.iconWrapper}>
          <FiUsers size={36} color="#C084FC" />
        </div>
      ),
      title: "커뮤니티",
      description: "다른 반려동물 애호가들과 팁을 공유하고 소통하세요",
      color: "#C084FC",
      link: "/community", // 아직 구현되지 않은 페이지이지만 향후 구현 예정
    },
  ];

  return (
    <section className={styles.featureCardsSection}>
      <div className="container">
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>당신이 필요한 모든 것</h2>
        </div>

        <div className={styles.cardsGrid}>
          {cards.map((card) => (
            <Link key={card.id} href={card.link} className={styles.cardLink}>
              <div
                className={styles.card}
                style={{ borderTopColor: card.color }}
              >
                <div className={styles.cardIcon}>{card.icon}</div>

                <h3 className={styles.cardTitle}>{card.title}</h3>

                <p className={styles.cardDescription}>{card.description}</p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
