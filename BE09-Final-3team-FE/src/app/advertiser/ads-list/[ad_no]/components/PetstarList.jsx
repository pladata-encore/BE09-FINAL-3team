"use client"

import React, { useState } from 'react';
import Image from 'next/image';
import styles from '../styles/PetstarList.module.css';
import PortfolioModal from './PortfolioModal';

export default function PetstarList({ petstars, currentPage, onPageChange, sortBy, onSortChange }) {
  const [isPortfolioModalOpen, setIsPortfolioModalOpen] = useState(false);
  const [selectedPetData, setSelectedPetData] = useState(null);

  const handleSortChange = (e) => {
    onSortChange(e.target.value);
  };

  return (
    <div className={styles.container}>
      {/* Header */}
      <div className={styles.header}>
        <h2 className={styles.title}>펫스타 추천 목록</h2>
        <div className={styles.controls}>
          <select 
            className={styles.sortSelect} 
            value={sortBy} 
            onChange={handleSortChange}
          >
            <option value="name">이름순</option>
            <option value="follower">팔로워 수</option>
            <option value="price">가격순</option>
          </select>
          <div className={styles.totalCount}>
            <span>총 12 마리</span>
          </div>
        </div>
      </div>

      {/* Petstar Grid */}
      <div className={styles.petstarGrid}>
        {petstars.map((petstar) => (
          <div key={petstar.id} className={styles.petstarCard}>
            <div className={styles.petstarImage}>
              <Image 
                src={petstar.image} 
                alt={petstar.name}
                width={262}
                height={256}
                className={styles.image}
              />
            </div>
            <div className={styles.petstarInfo}>
              <div className={styles.petstarDiv}>
                <h3 className={styles.petstarName}>{petstar.name}</h3>
                <p className={styles.petstarUsername}>{petstar.sns_profile}</p>
              </div>
              <p className={styles.petstarDescription}>{petstar.description}</p>
              <div className={styles.followerInfo}>
                <svg width="20" height="16" viewBox="0 0 20 16" fill="none">
                  <path d="M10 8C12.21 8 14 6.21 14 4C14 1.79 12.21 0 10 0C7.79 0 6 1.79 6 4C6 6.21 7.79 8 10 8ZM10 10C7.33 10 2 11.34 2 14V16H18V14C18 11.34 12.67 10 10 10Z" fill="#6B7280"/>
                </svg>
                <span>팔로워 수: {petstar.followers}</span>
              </div>

              <div className={styles.priceInfo}>
                <Image 
                  src="/advertiser/won.png"
                  alt="won.png"
                  width={16}
                  height={16}/>
                <span>{petstar.price}/건</span>
              </div>
              <div className={styles.actionButtons}>
                <button className={styles.snsButton}>
                  <Image 
                    src="/advertiser/sns.png"
                    alt="sns.png"
                    width={16}
                    height={16}/>
                  SNS
                </button>
                <button 
                  className={styles.portfolioButton}
                  onClick={() => {
                    if (petstar.name === "황금이") {
                      const petData = {
                        name: '황금이',
                        breed: '골든 리트리버',
                        age: '3살',
                        weight: '28 kg',
                        gender: 'M',
                        personality: '황금이는 매우 친근하고 사교적인 성격으로, 사람뿐 아니라 다른 반려동물과도 쉽게 친해집니다. 긍정적인 에너지와 활발한 호기심으로 언제나 주변 분위기를 밝게 만드는 친구입니다. 상황에 따라 차분함과 활동성을 유연하게 조절하는 균형 잡힌 성격을 가지고 있어 다양한 환경에 잘 적응합니다.',
                        introduction: '모험을 사랑하는 황금이는 해변 산책과 산악 하이킹 코스를 즐기며 자연 속에서 뛰노는 걸 가장 좋아합니다. 친구들과 어울려 뛰어노는 것을 즐기며, 특히 어린이와 다른 반려동물들과 따뜻한 교감을 나누는 모습을 자주 볼 수 있습니다. 신뢰감 있고 충성스러운 성향 덕분에 가족들의 든든한 친구이자 보호자로도 사랑받고 있습니다.',
                        addSection: '황금이는 민감성 피부와 소화기를 가지고 있어, 화학 첨가물이 없는 유기농 사료를 꾸준히 찾아왔습니다. 다양한 프리미엄 사료를 체험하며 비교·리뷰한 경험이 있어, 이번 제품의 장점을 정확히 전달할 자신이 있습니다. 특히 활동량이 많아 균형 잡힌 단백질과 오메가 지방산 공급이 큰 도움이 될 것이라 기대합니다.',
                        image: '/user/dog.png',
                        instagram: '@goldenbuddy',
                        followers: '189K',
                        price: '250,000',
                        partcipation: 10
                      };
                      setSelectedPetData(petData);
                      setIsPortfolioModalOpen(true);
                    }
                  }}
                >
                  <Image 
                    src="/advertiser/folder.png"
                    alt="folder.png"
                    width={16}
                    height={16}/>
                  포트폴리오
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Pagination */}
      <div className={styles.pagination}>
        {/* 왼쪽 이동 버튼 */}
        <button
          className={styles.pageButton}
          onClick={() => {
            if (currentPage > 1) {
              onPageChange(currentPage - 1);
            }
          }}
          disabled={currentPage === 1} // 첫 페이지일 땐 비활성화
        >
          <svg width="8" height="14" viewBox="0 0 8 14" fill="none">
            <path
              d="M7 1L1 7L7 13"
              stroke="#6B7280"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>

        {/* 페이지 번호 버튼 */}
        {[1, 2, 3, 4].map((page) => (
          <button
            key={page}
            className={`${styles.pageButton} ${currentPage === page ? styles.activePage : ''}`}
            onClick={() => onPageChange(page)}
          >
            {page}
          </button>
        ))}

        {/* 오른쪽 이동 버튼 */}
        <button
          className={styles.pageButton}
          onClick={() => {
            if (currentPage < 4) { // 마지막 페이지 번호에 맞게 변경
              onPageChange(currentPage + 1);
            }
          }}
          disabled={currentPage === 4} // 마지막 페이지일 땐 비활성화
        >
          <svg width="8" height="14" viewBox="0 0 8 14" fill="none">
            <path
              d="M1 1L7 7L1 13"
              stroke="#6B7280"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
      </div>

      {/* Portfolio Modal */}
      <PortfolioModal
        isOpen={isPortfolioModalOpen}
        onClose={() => setIsPortfolioModalOpen(false)}
        petData={selectedPetData}
      />
   </div>
  );
}

