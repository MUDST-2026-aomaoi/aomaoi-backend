--liquibase formatted sql

--changeset auto:latest-data runOnChange:true
--comment: Automatically exported latest database records
--rollback TRUNCATE TABLE work_logs, audit_logs, admin_profile, worker, farm, users, workers, farms RESTART IDENTITY CASCADE;
--
-- PostgreSQL database dump
--


-- Dumped from database version 15.19
-- Dumped by pg_dump version 15.19

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.users VALUES (1, 'superadmin', '$2a$10$XRmxht1Z/k/4QVkumFveFuX8QLLW9jSH60Al3.OH2bOX3Cos/9A7a', 'superadmin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (2, 'admin1', '$2a$10$URVIsGf72Wa9K0OYb6PwweHVDs4Y4YahyZBND2ld5vgfvmnT047W6', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (3, 'worker1', '$2a$10$3E7j0dO.9ql0W2FpKSg8LeeuPAHmrAdV2mD5zDmwE.flRTGxlX3xq', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (4, 'worker2', '$2a$10$IbQ9W05bYfaMvg24ao529ult3NIeqT2pR1c2Ha7GOi8WrYP8jta0q', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (5, 'worker3', '$2a$10$dOg7zwVv.OR1VeTCSuSopeYepgo1z3juruXS4TS93s2oCi1xWnfgW', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (6, 'admin2', '$2a$10$XYQJxhmtc7U6V5Y2EreoyeTns489iBm93.UHcnEJuyMSstickmboG', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (7, 'admin3', '$2a$10$nfkqR.CIqc8WsPEZ8qTUY.kwhuMN37nGmS.yctrh3LtnH.feoi3v.', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (8, 'admin4', '$2a$10$DUTJ.WwVatSXUj17l9CGROilthIMxO1sO70f2AMsGp0F1.wtOaMNe', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (9, 'admin5', '$2a$10$v3yn/WsOZTGmcPmkXQGO/erGKXYkBxmtAvqX4bmT0vr17K.yCJuOC', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (10, 'admin6', '$2a$10$9yQ8rywn8hCBsnFKtsNxk.5GTHzREYoVf4gCB6xAKp4R4GikSZjCS', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (11, 'e2e_adm_mv8exv864l', '$2a$10$AWLFBNmIGLTzzq59iHqg3Onbzvmblbig9YXXmIRmgAi1dAZdCpjvu', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (12, 'e2e_wkr_mv8exv864l', '$2a$10$yOzzkFkguQ9vftd/CIx2IuXdmLfEMvEoD7yoNRE.gKL/GTnRWcweO', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (13, 'e2e_adm_mv8exx0pgh', '$2a$10$OAf6UW.S4Dluaak10p/yh.AayNJaztPtQFcv957s1/Ox2CsaX0wMG', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (14, 'e2e_wkr_mv8exx0pgh', '$2a$10$J8PBwLq5mZFjqmUKSvqTdOutnWsbph.K7rGR3TMtdQ3wtPY8os7YK', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (15, 'e2e_adm_mv8exy7ihm', '$2a$10$Nv45CY/pYXU/fodSHayxSum6BgGEZPjWyZw6IAfLcvxeQxLBiWXkm', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (16, 'e2e_wkr_mv8exy7ihm', '$2a$10$KGDMSSn5g8y9r5epH1pSaOayihdLIxre38svHZ.B1Mo6togWTJAxy', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (17, 'e2e_adm_mv8exz9p21', '$2a$10$1ETmJJJWc2Tz4VEiAx6TF.uZYWXgcKqZMLcpDPjDhZU3pE61KDI0S', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (18, 'e2e_adm_mv8f4kdqd5', '$2a$10$.zD.4GmN0mUITke81vnzl.hjGXSoIZgnUIn/VdeFww6gSQTAWJgGC', 'admin', true) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (19, 'e2e_adm_mv8f4kqqbk', '$2a$10$NmZdxfVRx3mPNXDvnsuPdOFjotN.OTk6SwcN4RipVF7zaXeapI3Ca', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (20, 'e2e_wk_hist_mv8f4kqo9p', '$2a$10$CtChuhzocluF/rzU.1LNTO1DefJGVGg50EMIx.Eoocypr/0ZbA49.', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (21, 'e2e_adm_mv8f4ml0fi', '$2a$10$Sz3NoaqAVZNlgHt5NXg32.6Zwx9VPYxpCQmvBY8rD/D3FDSL0nKPm', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (22, 'e2e_wk_hist_mv8f4mkzip', '$2a$10$4zMYjEtm79MrHblw4Z9I3e89NSmgBc90wAy7JpyVjOXYXWEyf0Zp.', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (23, 'e2e_adm_mv8f4o5b3e', '$2a$10$x5NDW/2fwivHfqDUND7RoeO9N1iVN2rpOjaWlwnEzxWAA3AZvjCfe', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (24, 'e2e_wk_hist_date_mv8f4o5an', '$2a$10$fPphgo98411cETWKZG85muoI59AEA6cknkXUNa1fXI6F3SYZrI/Ji', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (25, 'e2e_adm_mv8f4vw8oh', '$2a$10$hvqWJeJtn7Moa8QSMKMk0O1o0x6y3CSiyPFIi9pKKzS.7O1zvhz.C', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (26, 'e2e_wkr_mv8f4vw8oh', '$2a$10$Ihw2Jr5yfFn/h43JK5pOfe/GaW2dHyAfowS80Pzy81F/cD.A4HUy6', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (27, 'e2e_adm_mv8f4wtrll', '$2a$10$NZmFZUvmpkLRajwIEnVxpO0cKH3v/HaywYRbzn7Xx2xlkSvz60ZKW', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (28, 'e2e_wkr_mv8f4wtrll', '$2a$10$ek73vK9ck0gVcIPWVc3cqeHDTYtJzZrGdZgsdLaG.3wvJx1dY/TOq', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (29, 'e2e_adm_mv8f4xtqli', '$2a$10$52RfMiexg6YN/fyiyW5mZ.rEVOxnj6D.sHMm4gFktLnajDqVyLhXO', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (30, 'e2e_wkr_mv8f4xtqli', '$2a$10$EmTTnCWITOqnPIjhUvRSp.IQ4/A9tCBC6iBklgOfcXPb6lhKCSwMq', 'worker', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (31, 'e2e_adm_mv8f4yqj7b', '$2a$10$PZ0EwE2X63a5ySHkxpBob.o7s8oNj.pPOUyotpo50ei67sdz7jTAG', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (32, 'e2e_adm_mv8f53tw1z', '$2a$10$ghbrXd5p/vNrAzIOvhdCGu3NZt6W.6C6A5J1EopcKC8SIitXq81Iq', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (33, 'e2e_adm_mv8f578hoo', '$2a$10$oEX1Np0jUgr/sKOZYSDdh.e8nTYmQngFOy4Xd/hp7dS4/sCTJExZ2', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (34, 'e2e_adm_mv8f58327m', '$2a$10$cEIbcfuvhZwTLjR4p8TtXOspulx66eMYRq7HUmIoV1S.yggc/gwUG', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (35, 'e2e_adm_mv8f591qb3', '$2a$10$K7wS6eeh5aQ6YuuV2j.BSOOgGG7VD1A19qq7GyXbVCeyqogVBSCTO', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (36, 'e2e_adm_mv8f59wri1', '$2a$10$mPr3VWwWlHYZji5hip9STu20iKvlUt8yEeMf0oPocgKm1trdvHQH2', 'admin', false) ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (37, 'e2e_adm_mv8f5ap3bv', '$2a$10$hLVuDpm8u75flGYVU/aVVOO118vL54Z4Qbsrwgf.oPxpvk5kRcmOu', 'admin', false) ON CONFLICT DO NOTHING;


--
-- Data for Name: admin_profile; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.admin_profile VALUES (1, 'สมศักดิ์ รักฟาร์ม', '0812345678', '1', NULL, 'active', '2026-07-13', 2) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (2, 'สมสู่ อาชา', '1234567890', '1', NULL, 'active', '2026-10-13', 6) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (3, 'สมชา อาสู่', '3216549870', '1', NULL, 'active', '2026-10-13', 7) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (4, 'สมรักษ์ คำสร้อย', '3456789012', '1', NULL, 'active', '2026-10-13', 8) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (5, 'สมหมาย คลายร้อน', '2034987543', '1', NULL, 'active', '2026-10-13', 9) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (6, 'สมปอง ครองใจ', '0239487554', '1', NULL, 'active', '2026-10-13', 10) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (7, 'แอดมิน mvexvl', '0812345678', '2', NULL, 'active', '2026-10-14', 11) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (8, 'แอดมิน mvexxpgh', '0812345678', '3', NULL, 'active', '2026-10-14', 13) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (9, 'แอดมิน mvexyihm', '0812345678', '4', NULL, 'active', '2026-10-14', 15) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (10, 'แอดมิน mvexzp', '0812345678', '5', NULL, 'active', '2026-10-14', 17) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (11, 'แอดมิน mvfkdqd', '0812345678', '6', NULL, 'pending', '2026-10-14', 18) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (12, 'แอดมิน mvfkqqbk', '0812345678', '7', NULL, 'active', '2026-10-14', 19) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (13, 'แอดมิน mvfmlfi', '0812345678', '8', NULL, 'active', '2026-10-14', 21) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (14, 'แอดมิน mvfobe', '0812345678', '9', NULL, 'active', '2026-10-14', 23) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (15, 'แอดมิน mvfvwoh', '0812345678', '10', NULL, 'active', '2026-10-14', 25) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (16, 'แอดมิน mvfwtrll', '0812345678', '11', NULL, 'active', '2026-10-14', 27) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (17, 'แอดมิน mvfxtqli', '0812345678', '12', NULL, 'active', '2026-10-14', 29) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (18, 'แอดมิน mvfyqjb', '0812345678', '13', NULL, 'active', '2026-10-14', 31) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (19, 'สมหญิง axpoqp', '0812345678', '14', NULL, 'active', '2026-10-14', 32) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (20, 'สมหญิง tkoabb', '0812345678', '15', NULL, 'active', '2026-10-14', 33) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (21, 'สมหญิง ziuxbo', '0812345678', '16', NULL, 'active', '2026-10-14', 34) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (22, 'สมหญิง bdurum', '0812345678', '17', NULL, 'active', '2026-10-14', 35) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (23, 'สมหญิง ugzijm', '0812345678', '18', NULL, 'active', '2026-10-14', 36) ON CONFLICT DO NOTHING;
INSERT INTO public.admin_profile VALUES (24, 'สมหญิง dlszxu', '0812345678', '19', NULL, 'active', '2026-10-14', 37) ON CONFLICT DO NOTHING;


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.audit_logs VALUES (1, 'RESET_PASSWORD', 'admin1', 'worker1', '2026-10-13 15:01:52.956316', 'Reset worker password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (2, 'CHANGE_PASSWORD', 'worker1', 'worker1', '2026-10-13 15:03:45.998641', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (3, 'CREATE_WORKER', 'admin1', 'worker3', '2026-10-13 15:04:47.856499', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (4, 'CHANGE_PASSWORD', 'worker3', 'worker3', '2026-10-13 15:05:10.676985', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (5, 'CREATE_ADMIN', 'superadmin', 'admin2', '2026-10-13 16:53:44.730082', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (6, 'CHANGE_PASSWORD', 'admin2', 'admin2', '2026-10-13 16:54:04.178874', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (7, 'CREATE_ADMIN', 'superadmin', 'admin3', '2026-10-13 17:16:44.861928', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (8, 'CHANGE_PASSWORD', 'admin3', 'admin3', '2026-10-13 17:16:59.418995', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (9, 'CREATE_ADMIN', 'superadmin', 'admin4', '2026-10-13 17:26:30.347545', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (10, 'CHANGE_PASSWORD', 'admin4', 'admin4', '2026-10-13 17:26:53.219807', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (11, 'CREATE_ADMIN', 'superadmin', 'admin5', '2026-10-13 17:34:42.343415', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (12, 'CHANGE_PASSWORD', 'admin5', 'admin5', '2026-10-13 17:35:00.464448', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (13, 'CREATE_ADMIN', 'superadmin', 'admin6', '2026-10-13 17:43:04.816366', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (14, 'CHANGE_PASSWORD', 'admin6', 'admin6', '2026-10-13 17:43:27.322564', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (15, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8exv864l', '2026-10-14 17:58:07.77199', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (16, 'CHANGE_PASSWORD', 'e2e_adm_mv8exv864l', 'e2e_adm_mv8exv864l', '2026-10-14 17:58:08.14049', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (17, 'CREATE_WORKER', 'e2e_adm_mv8exv864l', 'e2e_wkr_mv8exv864l', '2026-10-14 17:58:08.415705', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (18, 'CHANGE_PASSWORD', 'e2e_wkr_mv8exv864l', 'e2e_wkr_mv8exv864l', '2026-10-14 17:58:08.746904', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (19, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8exx0pgh', '2026-10-14 17:58:09.313571', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (20, 'CHANGE_PASSWORD', 'e2e_adm_mv8exx0pgh', 'e2e_adm_mv8exx0pgh', '2026-10-14 17:58:09.639577', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (21, 'CREATE_WORKER', 'e2e_adm_mv8exx0pgh', 'e2e_wkr_mv8exx0pgh', '2026-10-14 17:58:09.958496', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (22, 'CHANGE_PASSWORD', 'e2e_wkr_mv8exx0pgh', 'e2e_wkr_mv8exx0pgh', '2026-10-14 17:58:10.28009', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (23, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8exy7ihm', '2026-10-14 17:58:10.797121', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (24, 'CHANGE_PASSWORD', 'e2e_adm_mv8exy7ihm', 'e2e_adm_mv8exy7ihm', '2026-10-14 17:58:11.111466', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (25, 'CREATE_WORKER', 'e2e_adm_mv8exy7ihm', 'e2e_wkr_mv8exy7ihm', '2026-10-14 17:58:11.406539', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (26, 'CHANGE_PASSWORD', 'e2e_wkr_mv8exy7ihm', 'e2e_wkr_mv8exy7ihm', '2026-10-14 17:58:11.741923', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (27, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8exz9p21', '2026-10-14 17:58:12.187339', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (28, 'CHANGE_PASSWORD', 'e2e_adm_mv8exz9p21', 'e2e_adm_mv8exz9p21', '2026-10-14 17:58:12.51141', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (29, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f4kdqd5', '2026-10-14 18:03:19.468699', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (30, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f4kqqbk', '2026-10-14 18:03:19.907239', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (31, 'CHANGE_PASSWORD', 'e2e_adm_mv8f4kqqbk', 'e2e_adm_mv8f4kqqbk', '2026-10-14 18:03:20.209491', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (32, 'CREATE_WORKER', 'e2e_adm_mv8f4kqqbk', 'e2e_wk_hist_mv8f4kqo9p', '2026-10-14 18:03:20.421409', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (33, 'CHANGE_PASSWORD', 'e2e_wk_hist_mv8f4kqo9p', 'e2e_wk_hist_mv8f4kqo9p', '2026-10-14 18:03:20.848233', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (34, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f4ml0fi', '2026-10-14 18:03:22.327437', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (35, 'CHANGE_PASSWORD', 'e2e_adm_mv8f4ml0fi', 'e2e_adm_mv8f4ml0fi', '2026-10-14 18:03:22.638307', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (36, 'CREATE_WORKER', 'e2e_adm_mv8f4ml0fi', 'e2e_wk_hist_mv8f4mkzip', '2026-10-14 18:03:22.879544', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (37, 'CHANGE_PASSWORD', 'e2e_wk_hist_mv8f4mkzip', 'e2e_wk_hist_mv8f4mkzip', '2026-10-14 18:03:23.243933', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (38, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f4o5b3e', '2026-10-14 18:03:24.317003', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (39, 'CHANGE_PASSWORD', 'e2e_adm_mv8f4o5b3e', 'e2e_adm_mv8f4o5b3e', '2026-10-14 18:03:24.665567', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (40, 'CREATE_WORKER', 'e2e_adm_mv8f4o5b3e', 'e2e_wk_hist_date_mv8f4o5an', '2026-10-14 18:03:24.879233', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (41, 'CHANGE_PASSWORD', 'e2e_wk_hist_date_mv8f4o5an', 'e2e_wk_hist_date_mv8f4o5an', '2026-10-14 18:03:25.243843', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (42, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f4vw8oh', '2026-10-14 18:03:34.366156', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (43, 'CHANGE_PASSWORD', 'e2e_adm_mv8f4vw8oh', 'e2e_adm_mv8f4vw8oh', '2026-10-14 18:03:34.662573', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (44, 'CREATE_WORKER', 'e2e_adm_mv8f4vw8oh', 'e2e_wkr_mv8f4vw8oh', '2026-10-14 18:03:34.884924', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (45, 'CHANGE_PASSWORD', 'e2e_wkr_mv8f4vw8oh', 'e2e_wkr_mv8f4vw8oh', '2026-10-14 18:03:35.178568', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (46, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f4wtrll', '2026-10-14 18:03:35.623468', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (47, 'CHANGE_PASSWORD', 'e2e_adm_mv8f4wtrll', 'e2e_adm_mv8f4wtrll', '2026-10-14 18:03:35.955623', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (48, 'CREATE_WORKER', 'e2e_adm_mv8f4wtrll', 'e2e_wkr_mv8f4wtrll', '2026-10-14 18:03:36.180451', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (49, 'CHANGE_PASSWORD', 'e2e_wkr_mv8f4wtrll', 'e2e_wkr_mv8f4wtrll', '2026-10-14 18:03:36.471384', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (50, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f4xtqli', '2026-10-14 18:03:36.855772', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (51, 'CHANGE_PASSWORD', 'e2e_adm_mv8f4xtqli', 'e2e_adm_mv8f4xtqli', '2026-10-14 18:03:37.169038', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (52, 'CREATE_WORKER', 'e2e_adm_mv8f4xtqli', 'e2e_wkr_mv8f4xtqli', '2026-10-14 18:03:37.383064', 'Created new worker account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (53, 'CHANGE_PASSWORD', 'e2e_wkr_mv8f4xtqli', 'e2e_wkr_mv8f4xtqli', '2026-10-14 18:03:37.712438', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (54, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f4yqj7b', '2026-10-14 18:03:38.035477', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (55, 'CHANGE_PASSWORD', 'e2e_adm_mv8f4yqj7b', 'e2e_adm_mv8f4yqj7b', '2026-10-14 18:03:38.308114', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (56, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f53tw1z', '2026-10-14 18:03:44.673582', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (57, 'CHANGE_PASSWORD', 'e2e_adm_mv8f53tw1z', 'e2e_adm_mv8f53tw1z', '2026-10-14 18:03:48.021105', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (58, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f578hoo', '2026-10-14 18:03:49.041623', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (59, 'CHANGE_PASSWORD', 'e2e_adm_mv8f578hoo', 'e2e_adm_mv8f578hoo', '2026-10-14 18:03:49.318741', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (60, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f58327m', '2026-10-14 18:03:50.145686', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (61, 'CHANGE_PASSWORD', 'e2e_adm_mv8f58327m', 'e2e_adm_mv8f58327m', '2026-10-14 18:03:50.42307', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (62, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f591qb3', '2026-10-14 18:03:51.390502', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (63, 'CHANGE_PASSWORD', 'e2e_adm_mv8f591qb3', 'e2e_adm_mv8f591qb3', '2026-10-14 18:03:51.661233', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (64, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f59wri1', '2026-10-14 18:03:52.524518', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (65, 'CHANGE_PASSWORD', 'e2e_adm_mv8f59wri1', 'e2e_adm_mv8f59wri1', '2026-10-14 18:03:52.808125', 'User changed own password') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (66, 'CREATE_ADMIN', 'superadmin', 'e2e_adm_mv8f5ap3bv', '2026-10-14 18:03:53.55014', 'Created new admin account') ON CONFLICT DO NOTHING;
INSERT INTO public.audit_logs VALUES (67, 'CHANGE_PASSWORD', 'e2e_adm_mv8f5ap3bv', 'e2e_adm_mv8f5ap3bv', '2026-10-14 18:03:53.83438', 'User changed own password') ON CONFLICT DO NOTHING;


--
-- Data for Name: farm; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.farm VALUES (1, 'ไร่อ้อยอุดมทรัพย์ (Udomsap Sugarcane Farm)', 'กาญจนบุรี (Kanchanaburi)', NULL, 2, 1, 4550, 4550, 'active', '2026-07-13') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (2, 'E2E Farm mv8exv864l', 'นครปฐม', NULL, 1, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (3, 'E2E Farm mv8exx0pgh', 'นครปฐม', NULL, 1, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (4, 'E2E Farm mv8exy7ihm', 'นครปฐม', NULL, 1, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (5, 'E2E Farm mv8exz9p21', 'นครปฐม', NULL, 0, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (6, 'E2E Farm mv8f4kdqd5', 'นครปฐม', NULL, 0, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (7, 'E2E Farm mv8f4kqqbk', 'นครปฐม', NULL, 1, 1, 700, 700, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (8, 'E2E Farm mv8f4ml0fi', 'นครปฐม', NULL, 1, 1, 700, 700, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (9, 'E2E Farm mv8f4o5b3e', 'นครปฐม', NULL, 1, 1, 700, 700, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (10, 'E2E Farm mv8f4vw8oh', 'นครปฐม', NULL, 1, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (11, 'E2E Farm mv8f4wtrll', 'นครปฐม', NULL, 1, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (12, 'E2E Farm mv8f4xtqli', 'นครปฐม', NULL, 1, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (13, 'E2E Farm mv8f4yqj7b', 'นครปฐม', NULL, 0, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (14, 'E2E Admin Auth Farm mv8f53tw1z', 'นครปฐม', NULL, 0, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (15, 'E2E Admin Auth Farm mv8f578hoo', 'นครปฐม', NULL, 0, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (16, 'E2E Admin Auth Farm mv8f58327m', 'นครปฐม', NULL, 0, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (17, 'E2E Admin Auth Farm mv8f591qb3', 'นครปฐม', NULL, 0, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (18, 'E2E Admin Auth Farm mv8f59wri1', 'นครปฐม', NULL, 0, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;
INSERT INTO public.farm VALUES (19, 'E2E Admin Auth Farm mv8f5ap3bv', 'นครปฐม', NULL, 0, 1, 0, 0, 'active', '2026-10-14') ON CONFLICT DO NOTHING;


--
-- Data for Name: farms; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--



--
-- Data for Name: worker; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.worker VALUES (1, 'สมชาย หมายมั่น', 'ชาย', '0891112222', NULL, 'active', '1', '2026-08-13', 3) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (2, 'สมหญิง ยิ่งเจริญ', 'หญิง', '0893334444', NULL, 'active', '1', '2026-08-13', 4) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (3, 'สมสู่ อาชา', 'ม้า', '1234567890', NULL, 'active', '1', '2026-10-13', 5) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (4, 'สมชาย paltls', 'ชาย ejpibj', '0811111111', NULL, 'active', '2', '2026-10-14', 12) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (5, 'สมชาย sdulci', 'ชาย uvbebq', '0811111111', NULL, 'active', '3', '2026-10-14', 14) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (6, 'สมชาย ehgujj', 'ชาย lqctps', '0811111111', NULL, 'active', '4', '2026-10-14', 16) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (7, 'คนงานประวัติ qzwuzw', 'ประวัติ', '0999999999', NULL, 'active', '7', '2026-10-14', 20) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (8, 'คนงานประวัติ gdtowa', 'ประวัติ', '0999999999', NULL, 'active', '8', '2026-10-14', 22) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (9, 'คนงานวันที่ mpdlmf', 'วันที่', '0999999999', NULL, 'active', '9', '2026-10-14', 24) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (10, 'สมชาย brqico', 'ชาย ycajrt', '0811111111', NULL, 'active', '10', '2026-10-14', 26) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (11, 'สมชาย khkxju', 'ชาย bzqhqt', '0811111111', NULL, 'active', '11', '2026-10-14', 28) ON CONFLICT DO NOTHING;
INSERT INTO public.worker VALUES (12, 'สมชาย pxahwo', 'ชาย gbhqci', '0811111111', NULL, 'active', '12', '2026-10-14', 30) ON CONFLICT DO NOTHING;


--
-- Data for Name: work_logs; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--

INSERT INTO public.work_logs VALUES (1, 1, 'cutting', '2026-10-08', 10, 100, NULL, NULL, NULL, NULL, NULL, 2000.00, '2026-10-08', '2026-10-08') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (2, 1, 'planting', '2026-10-09', NULL, NULL, 15, 20, NULL, NULL, NULL, 750.00, '2026-10-09', '2026-10-09') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (3, 2, 'watering', '2026-10-10', NULL, NULL, NULL, NULL, 3, 350.00, NULL, 1050.00, '2026-10-10', '2026-10-12') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (4, 2, 'spraying', '2026-10-12', NULL, NULL, NULL, NULL, NULL, NULL, 5, 750.00, '2026-10-12', '2026-10-12') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (5, 7, 'planting', '2026-10-15', NULL, NULL, 5, 20, NULL, NULL, NULL, 250.00, '2026-10-15', '2026-10-15') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (6, 7, 'spraying', '2026-10-15', NULL, NULL, NULL, NULL, NULL, NULL, 3, 450.00, '2026-10-15', '2026-10-15') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (7, 8, 'planting', '2026-10-15', NULL, NULL, 5, 20, NULL, NULL, NULL, 250.00, '2026-10-15', '2026-10-15') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (8, 8, 'spraying', '2026-10-15', NULL, NULL, NULL, NULL, NULL, NULL, 3, 450.00, '2026-10-15', '2026-10-15') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (9, 9, 'planting', '2026-10-10', NULL, NULL, 5, 20, NULL, NULL, NULL, 250.00, '2026-10-10', '2026-10-10') ON CONFLICT DO NOTHING;
INSERT INTO public.work_logs VALUES (10, 9, 'spraying', '2026-10-20', NULL, NULL, NULL, NULL, NULL, NULL, 3, 450.00, '2026-10-20', '2026-10-20') ON CONFLICT DO NOTHING;


--
-- Data for Name: workers; Type: TABLE DATA; Schema: public; Owner: aomaoi_user
--



--
-- Name: admin_profile_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.admin_profile_id_seq', 24, true);


--
-- Name: audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.audit_logs_id_seq', 67, true);


--
-- Name: farm_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.farm_id_seq', 19, true);


--
-- Name: farms_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.farms_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.users_id_seq', 37, true);


--
-- Name: work_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.work_logs_id_seq', 10, true);


--
-- Name: worker_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.worker_id_seq', 12, true);


--
-- Name: workers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: aomaoi_user
--

SELECT pg_catalog.setval('public.workers_id_seq', 1, false);


--
-- PostgreSQL database dump complete
--


