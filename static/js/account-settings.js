(async function () {
  'use strict';
  const auth = window.ZipaiAuth;
  const section = document.getElementById('accountSettings');
  if (!auth || !section) return;
  await auth.ready;
  const user = auth.getUser();
  if (!user) return;
  const profile = document.getElementById('accountProfileForm');
  profile.elements.email.value = user.email || '';
  profile.elements.phone.value = user.phone || '';
  document.getElementById('accountUsernameForm').elements.userId.value = user.id || '';
  const message = document.getElementById('accountSettingsMessage');
  section.querySelectorAll('form[data-account-action]').forEach(function (form) {
    form.addEventListener('submit', async function (event) {
      event.preventDefault();
      const action = form.dataset.accountAction;
      const body = Object.fromEntries(new FormData(form));
      if (action === 'password' && body.newPassword !== body.confirmPassword) {
        message.textContent = '새 비밀번호와 확인 값이 다릅니다.';
        return;
      }
      if (action === 'withdraw' && !window.confirm('회원탈퇴를 진행할까요? 탈퇴 후 현재 계정으로 로그인할 수 없습니다.')) return;
      const button = form.querySelector('button[type="submit"]');
      button.disabled = true;
      message.textContent = '처리 중입니다.';
      try {
        const response = await fetch('/api/account' + (action === 'withdraw' ? '' : '/' + action), {
          method: action === 'withdraw' ? 'DELETE' : 'PUT', credentials: 'same-origin',
          headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
        });
        const result = await response.json().catch(function () { return {}; });
        if (!response.ok) throw new Error(result.message || '계정 변경에 실패했습니다.');
        form.querySelectorAll('input[type="password"]').forEach(function (input) { input.value = ''; });
        if (action === 'withdraw') {
          await auth.logout();
          window.location.href = '/member/login';
          return;
        }
        await auth.refreshUser();
        auth.updateLoginButtons();
        message.textContent = '변경이 완료되었습니다.';
        const heading = document.getElementById('mypageUserId');
        if (heading) heading.textContent = auth.getUser().id;
      } catch (error) { message.textContent = error.message; }
      finally { button.disabled = false; }
    });
  });
})();
