import { createRouter, createWebHistory } from 'vue-router';
import HomePage from './views/HomePage.vue';
import LoginPage from './views/LoginPage.vue';
import PublishPage from './views/PublishPage.vue';
import QaList from './views/QaList.vue';
import QaDetail from './views/QaDetail.vue';
import Recommend from './views/Recommend.vue';
import Profile from './views/Profile.vue';
import Followers from './views/Followers.vue';
import Notifications from './views/Notifications.vue';
import ContentDetail from './views/ContentDetail.vue';
import ReportPage from './views/ReportPage.vue';
import MyContent from './views/MyContent.vue';
import MePage from './views/MePage.vue';
import AdminPage from './views/AdminPage.vue';
import { getUserAuth, hasAdminSession } from './utils/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomePage },
    { path: '/login', component: LoginPage },
    { path: '/publish', component: PublishPage },
    { path: '/qa', component: QaList },
    { path: '/qa/:id', component: QaDetail },
    { path: '/content/:id', component: ContentDetail },
    { path: '/recommend', component: Recommend },
    { path: '/my-content', component: MyContent },
    { path: '/profile/:id?', component: Profile },
    { path: '/report', component: ReportPage },
    { path: '/followers', component: Followers },
    { path: '/notifications', component: Notifications },
    { path: '/me', component: MePage },
    { path: '/admin', component: AdminPage }
  ]
});

router.beforeEach((to) => {
  if (to.path.startsWith('/admin') && !hasAdminSession()) {
    return '/login?mode=admin';
  }
  const token = getUserAuth().token;
  if (to.path === '/me' && !token) {
    return '/login';
  }
  return true;
});

export default router;
