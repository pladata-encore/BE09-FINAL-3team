"use client";

import React, { useState, useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { FiUsers, FiList, FiUser } from "react-icons/fi";
import styles from "../styles/SideBar.module.css";
import { getAdvertiser, getFileByAdvertiserNo } from "@/api/advertiserApi";
import { useImage } from "../context/ImageContext";

const Sidebar = () => {
  const pathname = usePathname();
  const router = useRouter();

  if (pathname === "/advertiser") {
    return null;
  }

  const { imageVersion } = useImage();

  const navigationItems = [
    {
      id: "ads",
      label: "체험단 광고 목록",
      icon: <FiList className={styles.navIcon} />,
      href: "/advertiser/ads-list",
    },
    {
      id: "petstar",
      label: "펫스타 목록",
      icon: <FiUsers className={styles.navIcon} />,
      href: "/advertiser/petstar-list",
    },
    {
      id: "profile",
      label: "프로필 관리",
      icon: <FiUser className={styles.navIcon} />,
      href: "/advertiser/profile",
    },
  ];

  const [companyData, setCompanyData] = useState(null);
  const [previewImage, setPreviewImage] = useState(null);
  const [isLoadingImage, setIsLoadingImage] = useState(true);

  const DEFAULT_IMAGE_URL = `http://dev.macacolabs.site:8008/3/advertiser/images/default_brand.png`;

  // 네비게이션 클릭 핸들러
  const handleNavigationClick = (e, serviceName, href) => {
    // 광고주 페이지에서는 로그인 상태와 관계없이 이동 허용
    router.push(href);
  };

  const loadProfileImage = async () => {
    try {
      const fileData = await getFileByAdvertiserNo();

      const imageFile = fileData?.find(file => file.type === 'PROFILE');
      
      if (imageFile?.filePath && imageFile.filePath.trim() !== ""
      ) {
        setPreviewImage(imageFile.filePath);
        setIsLoadingImage(false);
      } else {
        setPreviewImage(DEFAULT_IMAGE_URL);
        setIsLoadingImage(false);
      }
    } catch (error) {
      console.error("Failed to load profile image:", error);
      setPreviewImage(DEFAULT_IMAGE_URL);
    }
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        const data = await getAdvertiser();
        setCompanyData(data);
      } catch (error) {
        console.error("Failed to get advertiser profile:", error);
      }
    };

    fetchData();
  }, []);

  useEffect(() => {
    loadProfileImage();
  }, [imageVersion]);

  return (
    <>
      <div className={styles.sidebar}>
        <div className={styles.sidebarContent}>
          <div className={styles.userProfile}>
            {isLoadingImage ? (
              <div></div>
            ) : (
              <>
                <img
                  src={previewImage || DEFAULT_IMAGE_URL}
                  alt="Advertiser"
                  className={styles.avatar}
                />
                <div className={styles.userName}>{companyData?.name}</div>
              </>
            )}
          </div>

          <nav className={styles.navigation}>
            {navigationItems.map((item) => (
              <a
                key={item.id}
                href={item.href}
                className={`${styles.navItem} ${
                  pathname.includes(item.href) ? styles.active : ""
                }`}
                onClick={(e) => handleNavigationClick(e, item.label, item.href)}
              >
                {item.icon}
                {item.label}
              </a>
            ))}
          </nav>
        </div>
      </div>
    </>
  );
};

export default Sidebar;
