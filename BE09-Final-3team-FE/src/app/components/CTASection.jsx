import Image from "next/image";
import styles from "../styles/CTASection.module.css";
import Link from "next/link";

export default function CTASection() {
  return (
    <section className={styles.ctaSection}>
      <div className="container">
        <div className={styles.ctaContent}>
          <h2 className={styles.ctaTitle}>
            PetFul에 가입할 준비가 되셨나요?
          </h2>
          <p className={styles.ctaDescription}>
            지금 바로 반려동물의 건강 및 SNS를 관리하고 반려동물의 영향력을
            키워보세요. <br/> 이미 수천 명의 반려동물 인플루언서들이 Petful에 함께하고
            있습니다.
          </p>
          <div className={styles.ctaButtonContainer}>
            <Link href="/user/signup" className={styles.ctaButton}>
              회원가입
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
